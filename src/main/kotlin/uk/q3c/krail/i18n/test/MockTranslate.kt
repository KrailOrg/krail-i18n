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

package uk.q3c.krail.i18n.test

import com.google.common.base.Joiner
import com.google.common.base.Preconditions.checkNotNull
import uk.q3c.krail.i18n.I18NKey
import uk.q3c.krail.i18n.Translate
import uk.q3c.krail.i18n.UnsupportedLocaleException
import uk.q3c.util.text.MessageFormatException
import uk.q3c.util.text.MessageFormatMode
import java.text.Collator
import java.util.*

/**
 * Very simple Mock for testing with Translate - just returns the I18NKey.name(), with underscores replaced by spaces, and with parameters and Locale and Locale
 * appended ... for example: <br></br>


 *
 *
 * Created by David Sowerby on 09 Feb 2016
 */
class MockTranslate : Translate {
    override fun from(strictness: MessageFormatMode, checkLocaleIsSupported: Boolean, key: I18NKey, locale: Locale, vararg arguments: Any): String {
        checkNotNull(key)
        checkNotNull(locale)
        if (checkLocaleIsSupported) {
            if (failOnCheckLocaleIsSupported) {
                throw UnsupportedLocaleException(locale)
            }
        }
        if (failOnStrictness) {
            throw MessageFormatException("Fake fail on strictness")
        }
        val n = (key as Enum<*>).name.replace('_', ' ')
        if (returnNameOnly) {
            return n
        } else {
            var args = ""
            if (arguments.isNotEmpty()) {
                args = "-${Joiner.on(",").join(arguments)}"
            }
            return "$n-${locale.toLanguageTag()}$args"
        }
    }

    var failOnCheckLocaleIsSupported = false
    var failOnStrictness = false
    var returnNameOnly = true


    override fun from(checkLocaleIsSupported: Boolean, key: I18NKey, locale: Locale, vararg arguments: Any): String {
        return from(MessageFormatMode.STRICT, checkLocaleIsSupported, key, locale, *arguments)
    }

    override fun from(key: I18NKey, locale: Locale, vararg arguments: Any): String {
        return from(true, key, locale, *arguments)
    }

    /**
     * Assumes [locale] is Locale.UK
     */
    override fun from(key: I18NKey, vararg arguments: Any): String {
        return from(true, key, Locale.UK, *arguments)
    }

    override fun collator(): Collator {
        throw RuntimeException("Not yet implemented")
    }


}
