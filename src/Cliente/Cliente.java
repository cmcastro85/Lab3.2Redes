package cliente;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

public class Cliente {

	private static final Logger LOGGER = Logger.getLogger(Cliente.class.getName());

	private static final String READY = "READY";

	protected MulticastSocket socket = null;
	protected DatagramSocket envio = null;
	protected byte[] buf = new byte[2100];
	protected int bufSize = 2048;

	private byte[] receive() {
		try {
			LOGGER.info("Recibiendo arhivo...");
			DatagramPacket dl = new DatagramPacket(buf, buf.length);
			socket.receive(dl);
			int lenght = Integer.parseInt(new String(dl.getData(), 0, dl.getLength()));
			byte[] imgbyte = new byte[lenght];
			byte[] mac;
			byte[] temp;
			byte[] ck;
			int i = 0;
			MessageDigest ms = MessageDigest.getInstance("MD5");
			while (i < lenght) {
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);

				if (lenght - i >= bufSize) {
					mac = Arrays.copyOfRange(packet.getData(), bufSize, packet.getLength());
					temp = Arrays.copyOfRange(packet.getData(), 0, bufSize);
					ck = ms.digest(temp);

					String ckS = DatatypeConverter.printHexBinary(ck).toUpperCase();
					String macS = DatatypeConverter.printHexBinary(mac).toUpperCase();

					if (ckS.equals(macS)) {
						System.arraycopy(temp, 0, imgbyte, i, temp.length);
					} else {
						LOGGER.log(Level.WARNING, "Hash incorrecto en el paquete");

					}
					i += bufSize;
				} else {
					mac = Arrays.copyOfRange(packet.getData(), packet.getLength()-16, packet.getLength());
					temp = Arrays.copyOfRange(packet.getData(), 0, lenght - i);
					ck = ms.digest(temp);

					String ckS = DatatypeConverter.printHexBinary(ck).toUpperCase();
					String macS = DatatypeConverter.printHexBinary(mac).toUpperCase();

					if (ckS.equals(macS)) {
						System.arraycopy(temp, 0, imgbyte, i, temp.length);
					} else {
						LOGGER.log(Level.WARNING, "Hash incorrecto en el paquete");
					}
					i += lenght - i;
				}
			}
			LOGGER.info("Archivo recibido!");
			return imgbyte;
		} catch (Exception e) {

			LOGGER.log(Level.SEVERE, "", e);
			return new byte[256];
		}
	}

	private void save(byte[] fileData) {
		try (FileOutputStream fos = new FileOutputStream(new File("/Users/Camilo/Desktop/Lab/video.jpg"));) {

			fos.write(fileData);
			LOGGER.info("Imagen creada!");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "No se pudo crear la imagen", e);
		}
	}

	public void run() {
		try {
			// * ---------------------- UNION -------------------

			LOGGER.info("Uniendose al grupo...");
			socket = new MulticastSocket(4446);
			envio = new DatagramSocket();
			InetAddress group = InetAddress.getByName("230.0.0.0");
			InetAddress server = InetAddress.getByName("localhost");
			socket.joinGroup(group);
			LOGGER.info("Union exitosa!");

			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Oprima 1 cuando este listo.");
			String linea = in.readLine();
			if (linea.equals("1")) {

				byte[] ready = READY.getBytes();

				DatagramPacket dp = new DatagramPacket(ready, ready.length, server, 5555);
				envio.send(dp);
			}
			envio.close();

			// * ---------------------- ENVIO ---------------------------
			byte[] imgbyte = this.receive();

			// *------------------ GUARDAR IMAGEN -----------------------
			this.save(imgbyte);

			socket.leaveGroup(group);
			socket.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error al recibir el archivo.", e);
		}
	}

	public static void main(String[] args) {
		Cliente cl = new Cliente();
		cl.run();
	}
}
