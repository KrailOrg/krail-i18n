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

import com.google.inject.Inject
import uk.q3c.krail.eventbus.MessageBus
import uk.q3c.krail.i18n.CurrentLocale
import uk.q3c.krail.i18n.LocaleChangeBusMessage
import java.util.*

/**
 * For testing all that is usually needed is to return a current locale and fire listeners on a change.  This class
 * gets used by so many things it makes test setup a pain if the full version is used.
 *
 * Note that you need to set [eventBus] externally if you want to use it, and reset it after deserialisation
 */
class MockCurrentLocale @Inject constructor() : CurrentLocale {
    @Transient
    var eventBus: MessageBus? = null
    override var locale: Locale = Locale.UK

    init {
        readFromEnvironment()
    }

    /**
     * Sets up the locale from the environment (typically browser locale and user option settings)
     */
    override fun readFromEnvironment() {
        setLocale(Locale.getDefault(), false)
    }

    override fun setLocale(locale: Locale, fireListeners: Boolean) {
        if (locale !== this.locale) {
            this.locale = locale
            if (fireListeners) {
                val eb = eventBus
                if (eb != null) {
                    eb.publishSync(LocaleChangeBusMessage(this, locale))
                }
            }
        }

    }


}
