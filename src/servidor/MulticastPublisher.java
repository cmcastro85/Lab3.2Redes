package servidor;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MulticastPublisher {

	/**
	 * Logger que mantiene el log de las transacciones.
	 */
	private static  Logger logger = Logger.getLogger(MulticastPublisher.class.getName());

	private static final String READY = "READY";
	
	private static final String PATH1 = "/Users/Camilo/Desktop/B99.mkv";
	
	private static final String PATH2 = "/Users/Camilo/Desktop/asdfmovie1-12.mp4";
	
	private static final String PATH3 = "/Users/Camilo/Desktop/asdfmovie.mp4";
	
	private static final String PATH4 = "/Users/Camilo/Desktop/hola.wmv";

	/**
	 * Socket de la conexion
	 */
	private DatagramSocket socket;
	
	private FileHandler fh;

	/**
	 * Ip del grupo
	 */
	private InetAddress group;


	private byte[] buff;

	/**
	 * Constructor de la clase Se inicia el socket de la conexón y el grupo.
	 */
	public MulticastPublisher() {
		try {
			socket = new DatagramSocket(5555);
			group = InetAddress.getByName("230.0.0.0");
			buff = new byte[7000];
			fh = new FileHandler("/Users/Camilo/Desktop/Log.log");  
	        logger.addHandler(fh);
	        SimpleFormatter formatter = new SimpleFormatter();  
	        fh.setFormatter(formatter);  
		} catch (SocketException e) {
			logger.log(Level.SEVERE, "El Socket falló.", e);
		} catch (UnknownHostException e) {
			logger.log(Level.SEVERE, "Group coul not be created.", e);
		} catch (SecurityException e) {
			logger.log(Level.SEVERE,"",e);
		} catch (IOException e) {
			logger.log(Level.SEVERE,"",e);
		}

	}

	/**
	 * Metodo para enviar un String
	 * 
	 * @param multicastMessage String a enviar
	 * @throws IOException
	 */
	private void multicast(String multicastMessage) throws IOException {

		byte[] buf = multicastMessage.getBytes();

		DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
		socket.send(packet);
	}

	/**
	 * Metodo para enviar un byte array
	 * 
	 * @param buf byte[] a enviar
	 * @throws IOException
	 */
	private void multicast(byte[] buf) throws IOException {

		DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
		socket.send(packet);
	}

	/**
	 * Metodo para cerrar la conexión.
	 */
	private void close() {
		logger.info("Acabando sesion.");
		socket.close();
	}

	/**
	 * Metodo para mandar los bytes del archivo.
	 * 
	 * @param data arreglo de bytes del archivo.
	 */
	private void sendFile(byte[] data) {

		try {
			logger.info("Creando hash...");
			MessageDigest ms = MessageDigest.getInstance("MD5");

			// *------------------ ENVIANDO ARCHIVO -----------------

			int size = data.length;
			multicast("" + size);
			int i = 0;
			byte[] mac;
			byte[] packet;
			long inicio = System.currentTimeMillis();
			logger.info("Comenzando transmición...");
			while (i < size) {
				byte[] buf;

				if (size - i >= buff.length) {
					buff = Arrays.copyOfRange(data, i, i + buff.length);
					mac = ms.digest(buff);
					packet = new byte[buff.length + mac.length];
					System.arraycopy(buff, 0, packet, 0, buff.length);
					System.arraycopy(mac, 0, packet, buff.length, mac.length);
					multicast(packet);
					TimeUnit.MICROSECONDS.sleep(2500);
					i += buff.length;
				} else {
					
					buf = Arrays.copyOfRange(data, i, size );
					mac = ms.digest(buf);
					packet = new byte[buf.length + mac.length];
					System.arraycopy(buf, 0, packet, 0, buf.length);
					System.arraycopy(mac, 0, packet, buf.length, mac.length);
					multicast(packet);
					i += size - i;
				}
			}
			logger.info("Archivo enviado!");

			double total = (System.currentTimeMillis() - inicio ) / 1000f;
			logger.info(() -> "TIEMPO DE TRANSMICIÓN: " + total + " SEGUNDOS");
			close();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error al mandar el archivo", e);

		} catch (NoSuchAlgorithmException e) {
			logger.log(Level.SEVERE, "Invalid hash algorithm", e);
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE, "", e);
			Thread.currentThread().interrupt();
		}

	}

	/**
	 * Metodo para esperar ususarios.
	 * 
	 * @param cl
	 */
	private void esperar() {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
			System.out.println("A cuantos clientes se les debe mandar el archivo ?");
			int cl;
			cl = Integer.parseInt(in.readLine());
			logger.info(() -> "Se le mandará el archivo a " + cl + " usuarios.");

			DatagramPacket dt = new DatagramPacket(buff, buff.length);

			int i = 0;
			while (i < cl) {
				logger.info("Esperando a los usuarios...");
				socket.receive(dt);
				String recibido = new String(dt.getData(), 0, dt.getLength());
				if (recibido.equals(READY)) {
					i++;
				}
			}
		} catch (NumberFormatException e) {
			logger.log(Level.SEVERE, "Inserte un número", e);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Formato invalido", e);
		}

	}

	/**
	 * Metodo para cargar archivos.
	 * 
	 * @param path path del archivo
	 * @return bytes del archivo
	 */
	private byte[] cargarArchivo(String path) {
		
		
		try (FileInputStream fis = new FileInputStream(path)) {
			logger.info("Cargando Archivo...");
			ByteArrayOutputStream output = new ByteArrayOutputStream();

			byte[] temp = new byte[4096];
			int n;
			while (-1 != (n = fis.read(temp))) {
				output.write(temp, 0, n);
			}

			return output.toByteArray();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "No se pudo cargar el archivo.", e);
			return new byte[1];
		}
	}

	public static void main(String[] args) {
		MulticastPublisher publisher = new MulticastPublisher();

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.println("¿Desea enviar el archivo 1 o el 2? ");
		String path = PATH4;
		try {
			String line = in.readLine();
			if(line.equals("1")) {
				 path = PATH1;
			}
			else if(line.equals("2")) {
				path = PATH2;
			}
			else if(line.equals("3")) {
				path = PATH3;
			}
			else path = PATH4;
		}catch(Exception e) {
			logger.log(Level.SEVERE,"Error al cargar el path",e);
		}
		
		byte[] file = publisher.cargarArchivo(path);
		publisher.esperar();

		

		publisher.sendFile(file);
	}

}