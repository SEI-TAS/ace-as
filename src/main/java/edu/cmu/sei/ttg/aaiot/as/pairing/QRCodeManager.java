package edu.cmu.sei.ttg.aaiot.as.pairing;

import javax.imageio.ImageIO;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;

import java.awt.image.BufferedImage;
import java.io.File;
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
        //LuminanceSource source = new BufferedImageLuminanceSource(ImageIO.read(new FileInputStream(filePath)));

        File file = new File(filePath);
        BufferedImage image = ImageIO.read(file);
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        RGBLuminanceSource source = new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels);

        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
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
