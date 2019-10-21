package cliente;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.xml.bind.DatatypeConverter;

public class Cliente {

	private static final Logger logger = Logger.getLogger(Cliente.class.getName());

	private static final String READY = "READY";

	protected MulticastSocket socket = null;
	protected DatagramSocket envio = null;
	protected byte[] buf = new byte[7016];
	protected int bufSize = 7000;
	protected FileHandler fh;
	
	public Cliente() {
		try {
			fh = new FileHandler("C:/temp/test/MyLogFile.log");
			logger.addHandler(fh);
	        SimpleFormatter formatter = new SimpleFormatter();  
	        fh.setFormatter(formatter); 
		} catch (SecurityException e) {
			logger.log(Level.SEVERE,"",e);
		} catch (IOException e) {
			logger.log(Level.SEVERE,"",e);
		}  
         
	}

	private byte[] receive() {
		try {
			logger.info("Esperando a recibir...");
			DatagramPacket dl = new DatagramPacket(buf, buf.length);
			socket.receive(dl);
			logger.info("Recibiendo arhivo...");
			int lenght = Integer.parseInt(new String(dl.getData(), 0, dl.getLength()));
			byte[] imgbyte = new byte[lenght];
			byte[] mac;
			byte[] temp;
			byte[] ck;
			int i = 0;
			MessageDigest ms = MessageDigest.getInstance("MD5");
			int numPaquetes =  (lenght/bufSize);
			double llegaron = 0;
			while (i < lenght) {
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);

				if (packet.getLength() > bufSize) {
					mac = Arrays.copyOfRange(packet.getData(), bufSize, packet.getLength());
					temp = Arrays.copyOfRange(packet.getData(), 0, bufSize);
					ck = ms.digest(temp);

					String ckS = DatatypeConverter.printHexBinary(ck).toUpperCase();
					String macS = DatatypeConverter.printHexBinary(mac).toUpperCase();

					if (ckS.equals(macS)) {
						System.arraycopy(temp, 0, imgbyte, i, temp.length);
					} else {
						logger.log(Level.WARNING, "Hash incorrecto en el paquete");

					}
					i += bufSize;
					llegaron++;
				} else {
					temp = Arrays.copyOfRange(packet.getData(), 0, lenght %bufSize);
					mac = Arrays.copyOfRange(packet.getData(), lenght%bufSize, packet.getLength());

					ck = ms.digest(temp);

					String ckS = DatatypeConverter.printHexBinary(ck).toUpperCase();
					String macS = DatatypeConverter.printHexBinary(mac).toUpperCase();
					llegaron++;
					if (ckS.equals(macS)) {
						System.arraycopy(temp, 0, imgbyte, i, temp.length);
					} else {
						logger.log(Level.WARNING, "Hash incorrecto en el paquete");
					}
					break;
				}
			}
			double porcentaje = (llegaron / numPaquetes) * 100;
			System.out.println("Llegaron "+porcentaje+" % de los paquetes");
			logger.info("Archivo recibido!");
			return imgbyte;
		} catch (Exception e) {

			logger.log(Level.SEVERE, "", e);
			return new byte[256];
		}
	}

	private void save(byte[] fileData) {
		try (FileOutputStream fos = new FileOutputStream(new File("/Users/Camilo/Desktop/Lab/video.wmv"));) {

			fos.write(fileData);
			logger.info("Imagen creada!");
		} catch (Exception e) {
			logger.log(Level.SEVERE, "No se pudo crear la imagen", e);
		}
	}

	public void run() {
		try {
			// * ---------------------- UNION -------------------

			logger.info("Uniendose al grupo...");
			socket = new MulticastSocket(4446);
			envio = new DatagramSocket();
			InetAddress group = InetAddress.getByName("230.0.0.0");
			InetAddress server = InetAddress.getByName("localhost");
			socket.joinGroup(group);
			logger.info("Union exitosa!");

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
			logger.log(Level.SEVERE, "Error al recibir el archivo.", e);
		}
	}

	public static void main(String[] args) {
		Cliente cl = new Cliente();
		cl.run();
	}
}
