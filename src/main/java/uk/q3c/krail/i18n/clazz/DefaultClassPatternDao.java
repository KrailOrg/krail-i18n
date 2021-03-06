/*
 *
 *  * Copyright (c) 2016. David Sowerby
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations under the License.
 *
 */

package uk.q3c.krail.i18n.clazz;

import com.google.inject.Inject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.q3c.krail.i18n.EnumResourceBundle;
import uk.q3c.krail.i18n.I18NKey;
import uk.q3c.krail.i18n.i18n.PatternDescriptionKey;
import uk.q3c.krail.i18n.i18n.PatternLabelKey;
import uk.q3c.krail.i18n.persist.PatternCacheKey;
import uk.q3c.krail.i18n.persist.PatternDao;
import uk.q3c.krail.i18n.persist.PatternWriteException;
import uk.q3c.krail.i18n.persist.clazz.ClassPatternDao;
import uk.q3c.krail.i18n.persist.clazz.ClassPatternSource;
import uk.q3c.krail.option.Option;
import uk.q3c.krail.option.OptionContext;
import uk.q3c.krail.option.OptionKey;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link PatternDao} implementation used with {@link EnumResourceBundle} instances held within code.  Writing back to source code is clearly not an option,
 * but this implementation can be set up to write source code files to a defined external directory.
 * <p>
 * Created by David Sowerby on 27/07/15.
 */
public class DefaultClassPatternDao implements ClassPatternDao, OptionContext {
    public static final String CONNECTION_URL = "Class based";
    public static final OptionKey<String> optionPathToValues = new OptionKey<>("", DefaultClassPatternDao.class, PatternLabelKey.Path, PatternDescriptionKey.Path);
    public static final OptionKey<Boolean> optionKeyUseKeyPath = new OptionKey<>(Boolean.TRUE, DefaultClassPatternDao.class, PatternLabelKey.Use_Key_Path,
            PatternDescriptionKey
                    .Use_Key_Path);
    private static Logger log = LoggerFactory.getLogger(DefaultClassPatternDao.class);
    protected Class<? extends Annotation> source;
    private ClassBundleControl control;
    private Option option;
    private File writeFile;


    @Inject
    protected DefaultClassPatternDao(ClassBundleControl control, Option option) {
        super();
        this.control = control;
        this.option = option;
        source = ClassPatternSource.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getWriteFile() {
        return writeFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWriteFile(File writeFile) {
        checkNotNull(writeFile);
        this.writeFile = writeFile;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
    @Override
    public Object write(PatternCacheKey cacheKey, String value) {
        checkNotNull(cacheKey);
        checkNotNull(value);
        if (writeFile == null) {
            throw new PatternWriteException("Write file must be set");
        }
        if (!writeFile.exists()) {
            throw new PatternWriteException("Write file must exist");
        }
        String indent = "    ";
        String indent2 = indent + indent;
        StringBuilder buf = new StringBuilder(indent2);
        buf.append("put(")
                .append(cacheKey.getKeyAsEnum()
                        .name())
                .append(", \"")
                .append(value)
                .append("\");\n");


        CharsetEncoder encoder = Charset.forName("UTF-8")
                .newEncoder();
        encoder.onMalformedInput(CodingErrorAction.REPORT);
        encoder.onUnmappableCharacter(CodingErrorAction.REPORT);

        String output = buf.toString();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(writeFile), encoder))) {
            writer.write(output);
            return output;
        } catch (Exception e) {
            throw new PatternWriteException("failed to write pattern", e);
        }

    }

    /**
     * {@inheritDoc}
     */

    @Override
    public Optional<String> deleteValue(PatternCacheKey cacheKey) {
        throw new UnsupportedOperationException("Class based I18NPatterns cannot be deleted");
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public Optional<String> getValue(PatternCacheKey cacheKey) {
        checkNotNull(cacheKey);
        // source is used to qualify the Option
        log.debug("getValue for cacheKey {}, source '{}', using control: {}", cacheKey, source, getControl().getClass()
                .getSimpleName());
        I18NKey key = cacheKey.getKey();
        String expandedBaseName = expandFromKey(key);
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(expandedBaseName, cacheKey.getActualLocale(), getControl());
            return Optional.of(getValue(bundle, cacheKey.getKeyAsEnum()));
        } catch (Exception e) {
            log.warn("returning empty value, as getValue() returned exception {} with message '{}'", e, e.getMessage());
            return Optional.empty();
        }
    }

    protected String getValue(ResourceBundle bundle, Enum<?> key) {
        EnumResourceBundle enumBundle = (EnumResourceBundle) bundle;
        //noinspection unchecked
        enumBundle.setKeyClass(key.getClass());
        enumBundle.load();
        //noinspection unchecked
        return enumBundle.getValue(key);
    }

    /**
     * Allows the setting of paths for location of class and property files.  The bundle base name is taken from {@link
     * I18NKey#bundleName()}.
     * <p>
     * {@link Option} entries determine how the bundle name is expanded.  If {@link #optionKeyUseKeyPath} is true, the bundle name is
     * appended to the package path of the {@code sampleKey}
     * <p>
     * If {@link #optionKeyUseKeyPath} is false, the bundle name is appended to {@link #optionPathToValues}
     *
     * @param sampleKey any key from the I18NKey class, to give access to bundleName()
     * @return a path constructed from the {@code sampleKey} and {@link Option} values
     */
    protected String expandFromKey(I18NKey sampleKey) {
        checkNotNull(sampleKey);
        String baseName = sampleKey.bundleName();
        String packageName;
        //use source to qualify the options, so they get their own, and not the base class
        if (option.get(optionKeyUseKeyPath.qualifiedWith(getSourceString()))) {
            packageName = ClassUtils.getPackageCanonicalName(sampleKey.getClass());

        } else {
            String pathOptionValue = option.get(optionPathToValues.qualifiedWith(getSourceString()));
            if (pathOptionValue.isEmpty() || ".".equals(pathOptionValue)) {
                packageName = ClassUtils.getPackageCanonicalName(sampleKey.getClass());
            } else {
                packageName = pathOptionValue;
            }
        }

        return packageName.isEmpty() ? baseName : packageName + '.' + baseName;
    }

    public String getSourceString() {
        return source.getSimpleName();
    }

    public ResourceBundle.Control getControl() {
        return control;
    }

    /**
     * Returns {@link DefaultClassPatternDao#CONNECTION_URL} as a connection url
     *
     * @return {@link DefaultClassPatternDao#CONNECTION_URL} as a connection url
     */
    @Override
    public String connectionUrl() {
        return DefaultClassPatternDao.CONNECTION_URL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long count() {
        throw new UnsupportedOperationException("count is not available for class based patterns");
    }


    /**
     * Returns the {@link Option} instance being used by this context
     *
     * @return the {@link Option} instance being used by this context
     */

    @Override
    public Option optionInstance() {
        return option;
    }
}
