package phd.research.helper;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Jordan Doyle
 */

public class DroidControlFactoryTest {

    DroidControlFactory factory;

    @Before
    public void setUp() {
        this.factory = new DroidControlFactory();
    }

    @Test
    public void isAndroidNamespace() {
        String androidNamespace = "http://schemas.android.com/apk/res/android";
        assertTrue("Namespace android should be accepted.", this.factory.isAndroidNamespace(androidNamespace));

        String resAutoNamespace = "http://schemas.android.com/apk/res-auto";
        assertTrue("Namespace res-auto should be accepted.", this.factory.isAndroidNamespace(resAutoNamespace));
        assertTrue("Namespace with * should be accepted.", this.factory.isAndroidNamespace("*" + resAutoNamespace));

        assertFalse("Null namespace accepted.", this.factory.isAndroidNamespace(null));
        assertFalse("Invalid namespace accepted.", this.factory.isAndroidNamespace(resAutoNamespace + "/invalid"));
    }
}