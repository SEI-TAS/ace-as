package edu.cmu.sei.ttg.aaiot.as.gui.controllers;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.NotFoundException;
import edu.cmu.sei.ttg.aaiot.as.Application;
import edu.cmu.sei.ttg.aaiot.as.AuthorizationServer;
import edu.cmu.sei.ttg.aaiot.as.pairing.PairingManager;
import edu.cmu.sei.ttg.aaiot.as.pairing.QRCodeManager;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

/**
 * Created by sebastianecheverria on 11/10/17.
 */
public class QRCaptureController
{
    @FXML
    ImageView imgWebCamCapturedImage;

    private Webcam webcam;
    private BufferedImage grabbedImage;
    private boolean stopCamera;
    private ObjectProperty<Image> imageProperty = new SimpleObjectProperty<>();

    private boolean stopReadingQR;

    @FXML
    public void initialize()
    {
        initializeWebCam();
    }

    /**
     * Opens the webcam in a separate thread.
     */
    protected void initializeWebCam()
    {
        Task<Void> webCamIntializer = new Task<Void>()
        {
            @Override
            protected Void call() throws Exception
            {
                if (webcam != null)
                {
                    webcam.close();
                }

                webcam = Webcam.getDefault();
                webcam.open();

                startWebCamStreamThread();
                System.out.println("Finished webcam setup");
                startGetQRAndPairThread();
                return null;
            }
        };
        new Thread(webCamIntializer).start();
    }

    public void startWebCamStreamThread()
    {
        stopCamera = false;
        Task<Void> task = new Task<Void>()
        {
            @Override
            protected Void call() throws Exception
            {
                System.out.println("Started webcam stream");
                while (!stopCamera)
                {
                    try
                    {
                        if ((grabbedImage = webcam.getImage()) != null)
                        {
                            Platform.runLater(() ->
                            {
                                final Image currentImage = SwingFXUtils.toFXImage(grabbedImage, null);
                                imageProperty.set(currentImage);
                            });

                            //grabbedImage.flush();

                            Thread.sleep(100);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }

                grabbedImage.flush();
                System.out.println("Finished webcam stream");
                return null;
            }
        };

        Thread th = new Thread(task);
        th.start();

        imgWebCamCapturedImage.imageProperty().bind(imageProperty);
    }

    public void stopWebCamStream()
    {
        stopCamera = true;
        if(webcam != null)
        {
            //webcam.close();
        }
    }

    public void cleanup()
    {
        System.out.println("Starting cleanup");
        stopReadingQR = true;
        stopWebCamStream();
        System.out.println("Finished cleanup");
    }

    /**
     * Tries to decode the current image as a QR code, and use it to pair.
     */
    public void startGetQRAndPairThread()
    {
        stopReadingQR = false;
        Task<Void> task = new Task<Void>()
        {
            @Override
            protected Void call() throws Exception
            {
                try
                {
                    System.out.println("Starting QR detection loop");
                    while(!stopReadingQR)
                    {
                        byte[] pskBytes = null;
                        if(grabbedImage != null)
                        {
                            System.out.println("Attempting to detect QR code");
                            ImageIO.write(grabbedImage, "JPG", new File(Application.QR_CODE_IMAGE_FILE_PATH));
                            System.out.println("Code written to image");
                            pskBytes = getQRBytes(Application.QR_CODE_IMAGE_FILE_PATH);
                        }

                        if (pskBytes != null)
                        {
                            // Perform pairing.
                            pairIoTDevice(pskBytes);

                            stopReadingQR = true;

                            // Whether it was successful or not, close the window.
                            Platform.runLater(() ->
                            {
                                Stage currStage = (Stage) imgWebCamCapturedImage.getScene().getWindow();
                                currStage.fireEvent(
                                        new WindowEvent(
                                                currStage,
                                                WindowEvent.WINDOW_CLOSE_REQUEST
                                        )
                                );
                            });
                        }
                        else
                        {
                            System.out.println("Waiting for a bit...");
                            Thread.sleep(1000);
                        }
                    }
                }
                catch(Exception e)
                {
                    System.out.println("Error processing QR code: " + e.toString());
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Error processing QR code: " + e.toString()).showAndWait();
                }

                return null;
            }
        };

        System.out.println("Starting QR detection");
        Thread th = new Thread(task);
        th.start();
    }

    /**
     * Parses a QR code from the given image.
     * @return The QR code as bytes.
     * @throws IOException
     */
    public byte[] getQRBytes(String filePath) throws IOException
    {
        byte[] contentInBytes = null;
        try
        {
            System.out.println("Reading QR code from image.");
            String qrCodeContents = QRCodeManager.readQRCode(filePath);
            System.out.println("QR code decoded: " + qrCodeContents);
            contentInBytes = Base64.getDecoder().decode(qrCodeContents);
        }
        catch(NotFoundException ex)
        {
            System.out.println("QR code not detected.");
        }
        return contentInBytes;
    }

    /**
     * Code to actually perform the pairing procedure with a client.
     */
    public boolean pairIoTDevice(byte[] pskBytes)
    {
        System.out.println("Started pairing");

        try
        {
            AuthorizationServer authorizationServer = Application.getInstance().getAuthorizationServer();
            String asId = authorizationServer.getAsId();
            PairingManager pairingManager = new PairingManager(authorizationServer);
            boolean success = pairingManager.pair(asId, pskBytes, Application.DEFAULT_DEVICE_IP);
            if(success)
            {
                Platform.runLater(() -> { new Alert(Alert.AlertType.INFORMATION, "Paired completed successfully.").showAndWait();});
                System.out.println("Finished pairing");
            }
            else
            {
                Platform.runLater(() -> { new Alert(Alert.AlertType.WARNING, "Pairing was aborted since device did not respond.").showAndWait();});
            }

            return success;
        }
        catch(Exception e)
        {
            System.out.println("Error pairing: " + e.toString());
            Platform.runLater(() -> { new Alert(Alert.AlertType.ERROR, "Error during pairing: " + e.toString()).showAndWait();});
            return false;
        }
    }
}

