/*
AAIoT Source Code

Copyright 2018 Carnegie Mellon University. All Rights Reserved.

NO WARRANTY. THIS CARNEGIE MELLON UNIVERSITY AND SOFTWARE ENGINEERING INSTITUTE MATERIAL IS FURNISHED ON AN "AS-IS"
BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY MATTER
INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE OR MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM
USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT MAKE ANY WARRANTY OF ANY KIND WITH RESPECT TO FREEDOM FROM
PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.

Released under a MIT (SEI)-style license, please see license.txt or contact permission@sei.cmu.edu for full terms.

[DISTRIBUTION STATEMENT A] This material has been approved for public release and unlimited distribution.  Please see
Copyright notice for non-US Government use and distribution.

This Software includes and/or makes use of the following Third-Party Software subject to its own license:

1. ace-java (https://bitbucket.org/lseitz/ace-java/src/9b4c5c6dfa5ed8a3456b32a65a3affe08de9286b/LICENSE.md?at=master&fileviewer=file-view-default)
Copyright 2016-2018 RISE SICS AB.
2. zxing (https://github.com/zxing/zxing/blob/master/LICENSE) Copyright 2018 zxing.
3. sarxos webcam-capture (https://github.com/sarxos/webcam-capture/blob/master/LICENSE.txt) Copyright 2017 Bartosz Firyn.
4. 6lbr (https://github.com/cetic/6lbr/blob/develop/LICENSE) Copyright 2017 CETIC.

DM18-0702
*/

package edu.cmu.sei.ttg.aaiot.as.gui.controllers;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.NotFoundException;
import edu.cmu.sei.ttg.aaiot.as.Application;
import edu.cmu.sei.ttg.aaiot.threads.TaskThread;
import edu.cmu.sei.ttg.aaiot.as.pairing.QRCodeManager;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
    @FXML ImageView imgWebCamCapturedImage;

    private Webcam webcam;
    private BufferedImage grabbedImage;
    private ObjectProperty<Image> imageProperty = new SimpleObjectProperty<>();

    private boolean stopCamera;
    private boolean stopReadingQR;

    private IPairingHandler pairingHandler;

    /**
     * Initializes the component, setting up threads for the webcam and QR processing.
     */
    @FXML
    public void initialize()
    {
        // Start webcome initialization in a task thread,.
        new TaskThread(() ->
        {
            try
            {
                // Open the webcam.
                if (webcam != null)
                {
                    webcam.close();
                }
                webcam = Webcam.getDefault();
                webcam.open();

                // Set up the camera stream.
                startWebCamStreamThread();
                System.out.println("Finished webcam setup");

                // Set up the QR processing thread.
                startGetQRAndPairThread();
            }
            catch (Exception e)
            {
                System.out.println("Error setting up camera: " + e.toString());
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Error setting up camera: " + e.toString()).showAndWait());
            }
        }).start();
    }

    /**
     * Sets the pairing handler.
     * @param pairingHandler
     */
    public void setPairingHandler(IPairingHandler pairingHandler)
    {
        this.pairingHandler = pairingHandler;
    }

    /**
     * Thread were webcam data is streamed into FX control.
     */
    public void startWebCamStreamThread()
    {
        stopCamera = false;
        new TaskThread(() ->
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
                            // Convert the image and set it in the FX control.
                            final Image currentImage = SwingFXUtils.toFXImage(grabbedImage, null);
                            imageProperty.set(currentImage);
                        });

                        // Small sleep to reduce load and allow QR processor to run in parallel.
                        Thread.sleep(100);
                    }
                }
                catch (Exception e)
                {
                    System.out.println("Error capturing camera image; ignoring.");
                    e.printStackTrace();
                }
            }

            if(grabbedImage != null)
            {
                grabbedImage.flush();
            }

            System.out.println("Finished webcam stream.");
        }).start();

        // Associate the FX control to the property manually.
        imgWebCamCapturedImage.imageProperty().bind(imageProperty);
    }

    /**
     * Used to stop the webcam stream thread.
     */
    public void stopWebCamStreamThread()
    {
        stopCamera = true;
        if(webcam != null)
        {
            //webcam.close();
        }
    }

    /**
     * Overall cleanup method for all threads and components.
     */
    public void cleanup()
    {
        System.out.println("Starting cleanup");
        stopReadingQR = true;
        stopWebCamStreamThread();
        System.out.println("Finished cleanup");
    }

    /**
     * Tries to decode the current image as a QR code, and use it to pair.
     */
    public void startGetQRAndPairThread()
    {
        stopReadingQR = false;
        new TaskThread(() ->
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
                        pskBytes = getQRBytes(Application.QR_CODE_IMAGE_FILE_PATH);
                    }

                    if (pskBytes != null)
                    {
                        // Perform pairing.
                        pairingHandler.pairIoTDevice(pskBytes);

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
                        // Sleep to allow user to move QR image so that it will be better captured.
                        System.out.println("Waiting for a bit...");
                        Thread.sleep(1000);
                    }
                }
            }
            catch(Exception e)
            {
                System.out.println("Error processing QR code: " + e.toString());
                e.printStackTrace();
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Error processing QR code: " + e.toString()).showAndWait());
            }
        }).start();
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

}

