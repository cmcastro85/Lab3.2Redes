package Cliente;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;



public class Cliente {

	private static final Logger LOGGER = Logger.getLogger(Cliente.class.getName());
	
	private static final String READY = "READY";

	protected MulticastSocket socket = null;
	protected DatagramSocket envio = null;
	protected byte[] buf = new byte[256];

	public void run() {
		try {
			//* ---------------------- UNION -------------------
			
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
			if(linea.equals("1")) {
				
				byte[] buf = READY.getBytes();
				
				DatagramPacket dp = new DatagramPacket(buf, buf.length,server,5555);
				envio.send(dp);
			}
			envio.close();
			
			//* ---------------------- ENVIO ---------------------------
			
			LOGGER.info("Recibiendo arhivo...");
			DatagramPacket dl = new DatagramPacket(buf, buf.length);
			socket.receive(dl);
			int lenght = Integer.parseInt(new String(dl.getData(),0,dl.getLength()));
			byte[] imgbyte = new byte[lenght];
			int i = 0;
			while( i<lenght) {
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				
				if(lenght-i>=256) {
					System.arraycopy(packet.getData(), 0, imgbyte, i, packet.getLength());
					i+=256;
				}
				else {
					System.arraycopy(packet.getData(), 0, imgbyte, i, packet.getLength());
					i += lenght -i;
				}
			}
			LOGGER.info("Archivo recibido!");
			
			
			//*----------------------- HASH ----------------------------
			
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			String check = new String(packet.getData(),0,packet.getLength());
			MessageDigest ms = MessageDigest.getInstance("MD5");
			
			byte [] hash = ms.digest(imgbyte);
			
			String hasho = DatatypeConverter.printHexBinary(hash).toUpperCase();
			
			if(check.equals(hasho)) {
				LOGGER.info("HASH CORRECTO!!");
			}
			else System.out.println("Error en el Hash!");
			
			//*------------------ GUARDAR IMAGEN -----------------------
			
			ByteArrayInputStream bis = new ByteArrayInputStream(imgbyte);
		    BufferedImage bImage2 = ImageIO.read(bis);
		    ImageIO.write(bImage2, "jpg", new File("/Users/Camilo/Desktop/Lab/foto.jpg") );
			LOGGER.info("Imagen creada!");
			socket.leaveGroup(group);
			socket.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Se murio el socket papá",e);
		}
	}
	
	public static void main(String[] args) {
		Cliente cl = new Cliente();
		cl.run();
	}
}   
 
