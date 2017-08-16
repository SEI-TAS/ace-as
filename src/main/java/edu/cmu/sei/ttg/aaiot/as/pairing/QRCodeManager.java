package edu.cmu.sei.ttg.aaiot.as.pairing;

import javax.imageio.ImageIO;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class QRCodeManager
{
    /***
     *
     * @param data
     * @param filePath
     * @throws IOException
     */
    public static void createQRCodeFile(String data, String filePath)
            throws IOException {
        FileOutputStream fos = new FileOutputStream(filePath, false);
        QRCode.from(data).to(ImageType.PNG).writeTo(fos);
        fos.close();
    }

    /**
     *
     * @param filePath
     *
     * @return data
     *
     * @throws IOException
     * @throws NotFoundException
     */
    public static String readQRCode(String filePath)
            throws IOException {
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(ImageIO.read(new FileInputStream(filePath)))));
        Result result = null;
        try
        {
            result = new MultiFormatReader().decode(binaryBitmap);
        } catch (NotFoundException e)
        {
            throw new IOException(e.toString());
        }
        return result.getText();
    }
}
