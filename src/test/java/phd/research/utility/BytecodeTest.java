package phd.research.utility;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Jordan Doyle
 */

@SuppressWarnings("SpellCheckingInspection")
public class BytecodeTest {

    @Test
    public void testSignatureToJimple() {
        String signatureWithZ = "Ljava/lang/Boolean;->parseBoolean(Ljava/lang/String;)Z";
        assertEquals("Wrong Z signature returned.", "<java.lang.Boolean: boolean parseBoolean(java.lang.String)>",
                Bytecode.signatureToJimple(signatureWithZ)
                    );

        String signatureWithB = "Landroid/os/Parcel;->readByte()B";
        assertEquals("Wrong B signature returned.", "<android.os.Parcel: byte readByte()>",
                Bytecode.signatureToJimple(signatureWithB)
                    );

        String signatureWithC = "Ljava/lang/String;-><init>([C)V";
        assertEquals("Wrong C signature returned.", "<java.lang.String: void <init>(char[])>",
                Bytecode.signatureToJimple(signatureWithC)
                    );

        String signatureWithS = "Lcom/google/ParcelReader;->readShort(Landroid/os/Parcel; I)S";
        assertEquals("Wrong S signature returned.", "<com.google.ParcelReader: short readShort(android.os.Parcel,int)>",
                Bytecode.signatureToJimple(signatureWithS)
                    );

        String signatureWithJ = "Landroid/os/Parcel;->writeLongArray([J)V";
        assertEquals("Wrong J signature returned.", "<android.os.Parcel: void writeLongArray(long[])>",
                Bytecode.signatureToJimple(signatureWithJ)
                    );

        String signatureWithF = "Ljava/lang/Float;->toString(F)Ljava/lang/String;";
        assertEquals("Wrong F signature returned.", "<java.lang.Float: java.lang.String toString(float)>",
                Bytecode.signatureToJimple(signatureWithF)
                    );

        String signatureWithD = "Landroid/os/Bundle;->putDouble(Ljava/lang/String; D)V";
        assertEquals("Wrong D signature returned.", "<android.os.Bundle: void putDouble(java.lang.String,double)>",
                Bytecode.signatureToJimple(signatureWithD)
                    );
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidBytecode() {
        Bytecode.signatureToJimple("");
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidPrimitive() {
        Bytecode.signatureToJimple("Landroid/os/Parcel;->readByte()b");
    }


}