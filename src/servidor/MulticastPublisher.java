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
import java.util.logging.Level;
import java.util.logging.Logger;

public class MulticastPublisher {

	/**
	 * Logger que mantiene el log de las transacciones.
	 */
	private static final Logger LOGGER = Logger.getLogger(MulticastPublisher.class.getName());

	private static final String READY = "READY";

	/**
	 * Socket de la conexion
	 */
	private DatagramSocket socket;

	/**
	 * Ip del grupo
	 */
	private InetAddress group;

	private int length;

	private byte[] buff;

	/**
	 * Constructor de la clase Se inicia el socket de la conexón y el grupo.
	 */
	public MulticastPublisher() {
		try {
			socket = new DatagramSocket(5555);
			group = InetAddress.getByName("230.0.0.0");
			length = 2048;
			buff = new byte[length];
		} catch (SocketException e) {
			LOGGER.log(Level.SEVERE, "El Socket falló.", e);
		} catch (UnknownHostException e) {
			LOGGER.log(Level.SEVERE, "Group coul not be created.", e);
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
		LOGGER.info("Acabando sesion.");
		socket.close();
	}

	/**
	 * Metodo para mandar los bytes del archivo.
	 * 
	 * @param data arreglo de bytes del archivo.
	 */
	private void sendFile(byte[] data) {

		try {
			LOGGER.info("Creando hash...");
			MessageDigest ms = MessageDigest.getInstance("MD5");

			// *------------------ ENVIANDO ARCHIVO -----------------

			int size = data.length;
			multicast("" + size);
			int i = 0;
			byte[] mac;
			byte[] packet;
			long inicio = System.currentTimeMillis();
			LOGGER.info("Comenzando transmición...");
			while (i < size) {
				byte[] buf;

				if (size - i >= length) {
					buf = Arrays.copyOfRange(data, i, i + length);
					mac = ms.digest(buf);
					packet = new byte[buf.length + mac.length];
					System.arraycopy(buf, 0, packet, 0, buf.length);
					System.arraycopy(mac, 0, packet, buf.length, mac.length);
					multicast(packet);
					TimeUnit.MILLISECONDS.sleep(2);
					i += length;
				} else {
					buf = Arrays.copyOfRange(data, i, i + size - i);
					mac = ms.digest(buf);
					packet = new byte[buf.length + mac.length];
					System.arraycopy(buf, 0, packet, 0, buf.length);
					System.arraycopy(mac, 0, packet, buf.length, mac.length);
					multicast(packet);
					i += size - i;
				}
			}
			LOGGER.info("Archivo enviado!");

			long total = (System.currentTimeMillis() - inicio) / 1000;
			LOGGER.info(() -> "TIEMPO DE TRANSMICIÓN: " + total + " SEGUNDOS");
			close();
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error al mandar el archivo", e);

		} catch (NoSuchAlgorithmException e) {
			LOGGER.log(Level.SEVERE, "Invalid hash algorithm", e);
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, "", e);
			Thread.currentThread().interrupt();
		}

	}

	/**
	 * Metodo para esperar ususarios.
	 * 
	 * @param cl
	 */
	private void esperar(int cl) {
		DatagramPacket dt = new DatagramPacket(buff, length);

		int i = 0;
		while (i < cl) {
			LOGGER.info("Esperando...");
			try {
				socket.receive(dt);
				String recibido = new String(dt.getData(), 0, dt.getLength());
				if (recibido.equals(READY)) {
					i++;
				}
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Error al recibir el ready", e);
			}
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
			LOGGER.info("Cargando Archivo...");
			ByteArrayOutputStream output = new ByteArrayOutputStream();

			byte[] temp = new byte[4096];
			int n;
			while (-1 != (n = fis.read(temp))) {
				output.write(temp, 0, n);
			}

			return output.toByteArray();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "No se pudo cargar el archivo.", e);
			return new byte[1];
		}
	}

	public static void main(String[] args) {
		MulticastPublisher publisher = new MulticastPublisher();

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("A cuantos clientes se les debe mandar el archivo ?");
		int i;
		try {
			i = Integer.parseInt(in.readLine());
			in.close();
			publisher.esperar(i);
		} catch (NumberFormatException e) {
			LOGGER.log(Level.SEVERE, "Inserte un número", e);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Formato invalido", e);
		}
		byte[] file = publisher.cargarArchivo("/Users/Camilo/Desktop/hola.wmv");

		publisher.sendFile(file);
	}

}