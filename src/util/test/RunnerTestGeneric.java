package util.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import c2.Commands;
import c2.Constants;
import c2.admin.LocalConnection;
import c2.session.SessionHandler;
import c2.session.SessionInitiator;
import c2.session.macro.persistence.WindowsHiddenUserMacro;
import c2.win.WindowsFileSystemTraverser;
import util.Time;
import util.test.TestConfiguration.OS;

public class RunnerTestGeneric {

	public static void testScreenshotsOnFS(String lang) {
		File dir = new File("test");
		File[] matches = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				try {
					String hostname = InetAddress.getLocalHost().getHostName().toUpperCase();
					return name.toUpperCase().equals(hostname + "-SCREEN");
				} catch (UnknownHostException e) {
					return false;
				}
				// test
			}
		});
		assertEquals(matches.length, 1);
		File[] userMatches = matches[0].listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.equals(System.getProperty("user.name"));
			}
		});
		assertEquals(userMatches.length, 1);
		File[] screenshots = userMatches[0].listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return true;
			}
		});
		assertTrue(screenshots.length >= 1);

		try {
			String hostname = InetAddress.getLocalHost().getHostName();
			if (lang.equals("C++")) {
				hostname = hostname.toUpperCase();
			}
			Path path = Paths.get("test", hostname + "-screen");
			if (Files.exists(path)) {
				Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			}
		} catch (IOException e2) {
			e2.printStackTrace();
			fail("Cannot clean up harvester");
		}
	}

	public static void connectionSetupGenericTwoClient(Socket remote, OutputStreamWriter bw, BufferedReader br,
			boolean testSecond) throws Exception {
		String output = br.readLine();
		assertEquals(output, SessionInitiator.AVAILABLE_SESSION_BANNER);
		output = br.readLine();
		assertTrue(output.contains("2:" + InetAddress.getLocalHost().getHostName())
				|| output.contains("2:" + InetAddress.getLocalHost().getHostName().toUpperCase()) || // C++ Daemon
				output.contains("3:" + InetAddress.getLocalHost().getHostName())
				|| output.contains("3:" + InetAddress.getLocalHost().getHostName().toUpperCase()) || // C++ Daemon
				output.equals("1:default:default:default"));
		System.out.println(output);
		System.out.println("Reading second session id...");
		output = br.readLine();
		System.out.println(output);
		assertTrue(output.contains("2:" + InetAddress.getLocalHost().getHostName())
				|| output.contains("2:" + InetAddress.getLocalHost().getHostName().toUpperCase()) || // C++ Daemon
				output.contains("3:" + InetAddress.getLocalHost().getHostName())
				|| output.contains("3:" + InetAddress.getLocalHost().getHostName().toUpperCase()) || // C++ Daemon
				output.equals("1:default:default:default"));

		System.out.println("Reading third session id...");
		output = br.readLine();
		System.out.println(output);
		assertTrue(output.contains("2:" + InetAddress.getLocalHost().getHostName())
				|| output.contains("2:" + InetAddress.getLocalHost().getHostName().toUpperCase()) || // C++ Daemon
				output.contains("3:" + InetAddress.getLocalHost().getHostName())
				|| output.contains("3:" + InetAddress.getLocalHost().getHostName().toUpperCase()) || // C++ Daemon
				output.equals("1:default:default:default"));

		output = br.readLine();
		assertEquals(SessionInitiator.WIZARD_BANNER, output);

		if (testSecond) {
			bw.write("3" + System.lineSeparator());
		} else {
			bw.write("2" + System.lineSeparator());
		}
		bw.flush();

		output = br.readLine();
		assertTrue(output.startsWith(SessionHandler.NEW_SESSION_BANNER));

	}

	public static void validateTwoSessionBanner(Socket remote, OutputStreamWriter bw, BufferedReader br,
			boolean isLinux, int baseIndex, boolean isRemote) throws IOException {
		String output = br.readLine();
		assertEquals(output, SessionInitiator.AVAILABLE_SESSION_BANNER);
		output = br.readLine();
		assertEquals("1:default:default:default", output);
		System.out.println("Reading second session id...");
		output = br.readLine();
		System.out.println("Daemon supplies: " + output);
		if (isLinux && !isRemote) {
			assertTrue(output
					.startsWith(baseIndex + ":" + InetAddress.getLocalHost().getHostName() + ":" + System.getProperty("user.name")));
		} else {
			if (isRemote) {
				assertTrue(output.contains(baseIndex + ":"));
			} else {
				assertTrue(output.contains(baseIndex + ":" + InetAddress.getLocalHost().getHostName())
						|| output.contains(baseIndex + ":" + InetAddress.getLocalHost().getHostName().toUpperCase()));// C++
																														// Daemon
			}
		}
		output = br.readLine();
		assertEquals(SessionInitiator.WIZARD_BANNER, output);
		System.out.println("Proper startup validated, continuing");
	}

	public static void connectionSetupGeneric(Socket remote, OutputStreamWriter bw, BufferedReader br, boolean isLinux,
			boolean isRemote) throws Exception {
		connectionSetupGeneric(remote, bw, br, isLinux, 2, isRemote);
	}

	public static void connectionSetupGeneric(Socket remote, OutputStreamWriter bw, BufferedReader br, boolean isLinux,
			int baseIndex, boolean isRemote) throws IOException {
		validateTwoSessionBanner(remote, bw, br, isLinux, baseIndex, isRemote);

		bw.write(baseIndex + System.lineSeparator());
		bw.flush();

		String output = br.readLine();
		assertTrue(output.startsWith(SessionHandler.NEW_SESSION_BANNER));

	}

	static void testLinuxLs(BufferedReader br, OutputStreamWriter bw) throws IOException {
		bw.write("ls" + System.lineSeparator());
		bw.flush();
		String output = br.readLine();
		assertEquals(output, "CMakeCache.txt");
		output = br.readLine();
		assertEquals(output, "CMakeFiles");
		output = br.readLine();
		assertEquals(output, "cmake_install.cmake");
		output = br.readLine();
		assertEquals(output, "CMakeLists.txt");
		output = br.readLine();
		assertEquals(output, "Common");
		output = br.readLine();
		assertEquals(output, "daemon");
		output = br.readLine();
		assertEquals(output, "daemon_dir");
		output = br.readLine();
		assertEquals(output, "dns_daemon");
		output = br.readLine();
		assertEquals(output, "Makefile");
		output = br.readLine();
		assertEquals(output, "punchlist");
		output = br.readLine();
		assertEquals(output, "test_uplink");
		output = br.readLine();
		assertEquals(output, "");
	}

	static int getFilesInFolder(String folder) {
		List<String> fileNames = new ArrayList<>();
		try {
			DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(folder));
			for (Path path : directoryStream) {
				fileNames.add(path.toString());
			}
		} catch (IOException ex) {
		}
		return fileNames.size();
	}

	static void testRootDirEnum(BufferedReader br, OutputStreamWriter bw) throws IOException {
		bw.write("dir" + System.lineSeparator());
		bw.flush();
		String output = br.readLine();
		assertEquals(" Volume in drive C is OS", output);
		output = br.readLine();
		assertTrue(output.startsWith(" Volume Serial Number is "));
		output = br.readLine();
		assertEquals("", output);
		output = br.readLine();
		assertEquals(" Directory of " + Paths.get("").toAbsolutePath().toString(), output);
		output = br.readLine();
		assertEquals("", output);
		output = br.readLine();
		assertTrue(output.contains("<DIR>          ."));
		output = br.readLine();
		assertTrue(output.contains("<DIR>          .."));
		// Sub out the hidden elements
		for (int idx = 0; idx < getFilesInFolder(Paths.get("").toAbsolutePath().toString()) - 2; idx++)
			br.readLine();

		output = br.readLine();
		if (!output.contains("bytes")) {
			output = br.readLine();
			if(!output.contains("bytes")){
				output = br.readLine();
				assertTrue(output.contains("bytes"));
			}
		}
		output = br.readLine();
		assertTrue(output.contains("bytes free"));
		output = br.readLine();
		assertEquals("", output);
	}

	static void testAddHiddenUsersError(BufferedReader br, OutputStreamWriter bw, String lang) throws IOException {
		System.out.println("Testing add_hidden_user error handling functionality");
		if(lang.equals("Python") || lang.equals("C++") || lang.equals("C#")) {
			OutputStreamWriterHelper.writeAndSend(bw, WindowsHiddenUserMacro.COMMAND);
			String output = br.readLine();
			if(lang.equals("Python")) {
				assertEquals("Cannot execute, errors encountered:", output);
			}else {
				assertTrue(output.startsWith("Cannot execute, errors encountered:"));
			}
			output = br.readLine();
			if(lang.equals("Python")) {
				assertEquals("Unable to add user: Unable to add user, not administrator", output);
			}else {//C++ & C#
				assertEquals("Unable to add user: Error: 5", output);
			}
			
			output = br.readLine();
			assertTrue(output.startsWith("Sent Command: 'add_hidden_user"));
			
			output = br.readLine();
			if(lang.equals("Python")) {
				assertEquals("Received response: 'Unable to add user, not administrator", output);
			}else {//C++ & C#
				assertEquals("Received response: 'Error: 5", output);
			}
			output = br.readLine();
			assertEquals("", output);
			if(lang.equals("C++")) {
				output = br.readLine();
				assertEquals("", output);
			}
			output = br.readLine();
			assertEquals("'", output);
			
			output = br.readLine();
			if(lang.equals("Python")) {
				assertEquals("Error: Unable to add user: Unable to add user, not administrator", output);
			}else {//C++ & C#
				assertEquals("Error: Unable to add user: Error: 5", output);
			}
		}
	}
	
	static void testCscDirEnum(BufferedReader br, OutputStreamWriter bw) throws IOException {
		bw.write("dir" + System.lineSeparator());
		bw.flush();
		String output = br.readLine();
		assertEquals(output, " Volume in drive C is OS");
		output = br.readLine();
		assertTrue(output.startsWith(" Volume Serial Number is "));
		output = br.readLine();
		assertEquals(output, "");
		output = br.readLine();
		assertEquals(output, " Directory of " + Paths.get("").toAbsolutePath().toString() + "\\agents\\csc");
		output = br.readLine();
		assertEquals(output, "");
		output = br.readLine();
		assertTrue(output.contains("<DIR>          ."));
		output = br.readLine();
		assertTrue(output.contains("<DIR>          .."));
		for (int idx = 0; idx < getFilesInFolder(Paths.get("").toAbsolutePath().toString() + "\\agents\\csc")
				- 2; idx++)
			br.readLine();
		output = br.readLine();
		System.out.println(output);
		assertTrue(output.contains("bytes"));
		output = br.readLine();
		assertTrue(output.contains("bytes free"));
		output = br.readLine();
		assertEquals(output, "");
	}

	private static String getRelativeRoot(TestConfiguration config) {
		String relativeRoot = "TheAllCommander";
		//if(config.lang.equals("C++")) {
		//	relativeRoot = "TheAllCommanderPrivate";
		//}
		return relativeRoot;
	}
	
	public static void test(TestConfiguration config) {

		Properties prop = new Properties();
		try (InputStream input = new FileInputStream("config" + File.separator + config.getServerConfigFile())) {

			// load a properties file
			prop.load(input);

		} catch (IOException ex) {
			System.out.println("RunnerTestGeneric: Unable to load TheAllCommander server config file");
			fail(ex.getMessage());
		}

		try {
			// This hack is b/c for some reason the C++ daemon doesn't create the dir on my
			// laptop
			Files.createDirectories(Paths.get(prop.getProperty(Constants.DAEMONLZHARVEST),
					InetAddress.getLocalHost().getHostName().toUpperCase() + "-screen",
					System.getProperty("user.name")));
			// end hack

			Files.deleteIfExists(Paths.get("System.Net.Sockets.SocketException"));
			Files.deleteIfExists(Paths.get("agents", "csc", "System.Net.Sockets.SocketException"));

			try {
				Thread.sleep(5000);// allow both commander and daemon to start
			} catch (InterruptedException e) {
				// Ensure that python client has connected
			}
			System.out.println("Connecting test commander...");
			Socket remote = LocalConnection.getSocket("127.0.0.1", 8012, ClientServerTest.getDefaultSystemTestProperties());
			System.out.println("Locking test commander streams...");
			OutputStreamWriter bw = new OutputStreamWriter(remote.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(remote.getInputStream()));

			// Ensure that python client has connected
			Time.sleepWrapped(500);

			System.out.println("Setting up test commander session...");
			try {
				if (config.isTestTwoClients()) {
					connectionSetupGenericTwoClient(remote, bw, br, config.isTestSecondaryClient());
				} else {
					connectionSetupGeneric(remote, bw, br, config.os == TestConfiguration.OS.LINUX, config.isRemote());
				}
			} catch (Exception ex) {
				fail(ex.getMessage());
			}

			if (config.protocol.equals("SMTP")) {
				String output = br.readLine();
				assertEquals(output, "Daemon alive");
			}

			testWindowsNoExtraSpacesOnNativeExecutableOutput(br, bw, config);
			
			//These functions are meant to support service based testing, which is not a goal of Java support
			if(!config.lang.equals("Java")) {
				testCpMv(br, bw, config);
				testLs(br, bw, config);
				testMkdirRmdir(br, bw, config);
				testRmDel(br, bw, config);
			}
			
			if (config.isExecInRoot()) {
				System.out.println("cd test");
				bw.write("cd test" + System.lineSeparator());
				bw.flush();
				bw.write("cd .." + System.lineSeparator());
				bw.flush();
				String output = br.readLine();
				String testAbsoluteLocation = Paths.get("test").toAbsolutePath().toString();
				if (!config.isRemote()) {
					assertEquals(testAbsoluteLocation, output);
					output = br.readLine();
					assertEquals(Paths.get("").toAbsolutePath().toString(), output);
				} else {
					String relativeRoot = getRelativeRoot(config);
					assertEquals(TestConstants.EXECUTIONROOT_REMOTE + "/" + relativeRoot + "/test", output);
					output = br.readLine();
					assertEquals(TestConstants.EXECUTIONROOT_REMOTE + "/" + relativeRoot, output);
				}

				if (!config.isRemote()) {
					System.out.println("cd absolute path test");
					OutputStreamWriterHelper.writeAndSend(bw, "cd " + testAbsoluteLocation);
					OutputStreamWriterHelper.writeAndSend(bw, "cd ..");
					output = br.readLine();
					assertEquals(testAbsoluteLocation, output);
					output = br.readLine();
					assertEquals(Paths.get("").toAbsolutePath().toString(), output);
				}
			}

			testWhereCommand(br, bw, config);

			System.out.println("getUID test");
			bw.write("getuid" + System.lineSeparator());
			bw.flush();

			String output = br.readLine();
			// System.out.println("Username: " + output);
			
				if (config.isRemote()) {
					assertTrue(output.startsWith("Username: "));
				} else {
					assertEquals("Username: " + System.getProperty("user.name"), output);
				}
			
			output = br.readLine();
			// System.out.println("Home Dir: " + output);
			if (config.lang.equals("C++")) {
				assertEquals(output, "Home Directory: Not supported");
			} else {
				if (config.os == TestConfiguration.OS.LINUX) {
					if (config.isRemote()) {
						assertTrue(output.startsWith("Home Directory: /home/"));
					}else {
						assertEquals(output, "Home Directory: /home/" + System.getProperty("user.name"));
					}
				}else if(config.os == TestConfiguration.OS.MAC){
					if (config.isRemote()) {
						assertTrue(output.startsWith("Home Directory: /Users/"));
					}else {
						assertEquals(output, "Home Directory: /Users/" + System.getProperty("user.name"));
					}
				} else {
					if (config.isRemote()) {
						assertTrue(output.startsWith("Home Directory: C:\\Users\\"));
					} else {
						assertEquals(output, "Home Directory: " + System.getProperty("user.home"));
					}
				}
			}
			output = br.readLine();
			// System.out.println("Hostname: " + output);
			if (config.isRemote()) {
				assertTrue(output.startsWith("Hostname: "));
			} else {
				String reference = "Hostname: " + InetAddress.getLocalHost().getHostName();
				assertEquals(reference.toUpperCase(), output.toUpperCase());
			}
			
			output = br.readLine();
			assertEquals(output, "");

			testWindowsCmdPromptObfuscation(br, bw, config);
			testWindowsPowershellPromptObfuscation(br, bw, config);
			
			testPwd(br, bw, config);

			testDownloadAndUplinkNominal(br, bw, config);

			testOSEnumeration(br, bw, config);

			testUplinkRandomBinaryFile(br, bw, config);
			if(config.os == OS.WINDOWS) {
				testUplinkDownloadWithSpaces(br, bw, config);
			}
			
			//Current screenshot library only works for Windows
			if (((config.lang.equals("C#") && !config.protocol.equals("DNS")) || config.lang.equals("C++")
					|| config.lang.equals("python") || config.lang.equals("Java"))
					&& config.os == TestConfiguration.OS.WINDOWS && config.isExecInRoot()) {
				System.out.println("Screenshot test");
				bw.write("screenshot" + System.lineSeparator());
				bw.flush();
				output = br.readLine();
				assertEquals(output, "Screenshot successful");

				testScreenshotsOnFS(config.lang);
			}

			//Clipboard API likewise only Windows
			if (config.os == TestConfiguration.OS.WINDOWS){
				testClipboard(br, bw, config);
			}

			if (config.isExecInRoot()) {
				testCat(br, bw, config);
			}

			testUplinkDownloadErrorHandling(br, bw);
			testCatErrorHandling(br, bw, config);
			
			if (!config.lang.equals("Java")) {
				testClientIdentifesExecutable(br, bw, config);
			} else {
				System.out.println("Java daemon prototype cannot identify executable arguments");
			}

			if (config.lang.equals("Native") || config.lang.equals("PowershellWindows")) {
				System.out.println("Native shell, cannot test sub shell spawning");
			} else {
				if (config.os != OS.WINDOWS) {
					System.out.println("Shell capability only working on Windows");
				} else if (config.lang.equals("C#")) {
					System.out.println("Shell capability not yet working on C#");
				} else if (config.lang.equals("Java")) {
					System.out.println("Shell capability not yet working on Java");
				}else if(config.protocol.equals("SMB")) {
					System.out.println("Shell capability not yet working on ");
				} else {
					System.out.println("Testing shell capability");
					testShell(br, bw, config);
				}

			}
			
			if(config.os == OS.WINDOWS) {
				testAddHiddenUsersError(br, bw, config.lang);
			}
			
			bw.write("die" + System.lineSeparator());
			bw.flush();

			bw.close();
			br.close();
			remote.close();

			Files.deleteIfExists(Paths.get("System.Net.Sockets.SocketException"));
			Files.deleteIfExists(Paths.get("localAgent", "csc", "System.Net.Sockets.SocketException"));
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}

		cleanup();
	}

	private static void testWindowsCmdPromptObfuscation(BufferedReader br, OutputStreamWriter bw, TestConfiguration config) throws IOException
	{
		if(config.os == OS.WINDOWS && !config.lang.equalsIgnoreCase("PowershellWindows")) {
			System.out.println("Testing obfuscated 'dir' command");
			OutputStreamWriterHelper.writeAndSend(bw, "<esc> dir");
			String output = br.readLine();
			assertTrue(output.startsWith(" Volume in drive"));
			//Flush the opening for dir
			for(int idx = 0; idx < 5; idx++) {
				output = br.readLine();
			}
			//iterate until we find the end
			if(config.lang.equals("Native")) {
				while (!output.contains("bytes free")) {
					output = br.readLine();
				}	
			}else {
			while (!output.equals("")) {
				output = br.readLine();
			}
			}
		}
	}
	
	private static void testWindowsPowershellPromptObfuscation(BufferedReader br, OutputStreamWriter bw, TestConfiguration config) throws IOException
	{
		if(config.os == OS.WINDOWS && !config.lang.equalsIgnoreCase("PowershellWindows") && !config.lang.equalsIgnoreCase("Native")) {
			System.out.println("Testing obfuscated 'powershell' command");
			OutputStreamWriterHelper.writeAndSend(bw, Commands.SESSION_START_OBFUSCATED_POWERSHELL_MODE);
			OutputStreamWriterHelper.writeAndSend(bw, "Get-Date -Format \"MM/dd/yyyy\"");
			OutputStreamWriterHelper.writeAndSend(bw, Commands.SESSION_END_OBFUSCATED_POWERSHELL_MODE);
			String output = br.readLine();
			assertEquals("Windows PowerShell", output);
			output = br.readLine();
			assertEquals("Copyright (C) Microsoft Corporation. All rights reserved.", output);
			output = br.readLine();//Blank
			output = br.readLine();//Try the new cross-platform PowerShell https://aka.ms/pscore6
			output = br.readLine();//Blank OR the Date response. Sometimes there's a command echo
			//And sometimes there isn't.
			if(output.length() != 0) {
				try {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
					LocalDate.parse(output, formatter);
				}catch(DateTimeParseException  ex) {
					fail("Native shell did not respond with PS prompt or valid date: " + output);
				}
			}else {
			//System.out.println("Polling for actual output");
			output = br.readLine();
			//Native system does not echo the command. It may or may not preface with a PS prompt
			boolean alreadySawDate = false;
			if(config.lang.startsWith("Native")) {
				if(!output.startsWith("PS")) {
					try {
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
						LocalDate.parse(output, formatter);
						alreadySawDate = true;
					}catch(DateTimeParseException  ex) {
						fail("Native shell did not respond with PS prompt or valid date: " + output);
					}
				}
			}else {
				assertTrue(output.startsWith("PS") && output.endsWith("Get-Date -Format \"MM/dd/yyyy\" "));
			}
			if(!alreadySawDate) {
				//System.out.println("Polling for second date attempt");
				output = br.readLine();
				if(output.startsWith("PS") && output.endsWith("> ")) {
					fail("Windows failed to respond to a date command poll. This happens occasionally, investigate if it becomes non-transient");
				}
				try {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
					LocalDate.parse(output, formatter);
				}catch(DateTimeParseException  ex) {
					fail("Attempted powershell command did not return real date: " + output);
				}
			}
			}
			System.out.println("Checking for final powershell prompt");
			output = br.readLine();
			assertTrue(output.startsWith("PS") && output.endsWith("> "));
			System.out.println("Powershell obfuscation test complete");
		}
	}
	
	private static void testOSEnumeration(BufferedReader br, OutputStreamWriter bw, TestConfiguration config)
			throws IOException {
		System.out.println("Testing client identifies OS heritage");
		OutputStreamWriterHelper.writeAndSend(bw, Commands.CLIENT_CMD_OS_HERITAGE);
		if (config.os == OS.WINDOWS) {
			assertEquals(Commands.OS_HERITAGE_RESPONSE_WINDOWS, br.readLine());
		}else if(config.os == OS.MAC) {
			assertEquals(Commands.OS_HERITAGE_RESPONSE_MAC, br.readLine());
		} else {//Linux
			assertEquals(Commands.OS_HERITAGE_RESPONSE_LINUX, br.readLine());
		}
	}

	private static void testWhereCommand(BufferedReader br, OutputStreamWriter bw, TestConfiguration config)
			throws IOException {
		if (config.os == OS.WINDOWS && !config.lang.equals("Native") && !config.lang.equals("PowershellWindows")  && !config.lang.equals("C#") && !config.lang.equals("Java") && !config.protocol.equals("SMB")) {
			System.out.println("Testing that where command returns an error on improper formatting");
			OutputStreamWriterHelper.writeAndSend(bw, "where /d");
			String output = br.readLine();
			assertEquals("Attempting search with 10 minute timeout", output);
			output = br.readLine();
			// if(config.lang.equalsIgnoreCase("Python")) {
			assertEquals("Cannot execute command Command 'where /d' returned non-zero exit status 2.", output);
			// }else {
			// assertEquals("ERROR: Invalid argument or option - '/d'.", output);
			// output = br.readLine();
			// assertEquals("Type \"WHERE /?\" for usage help.", br.readLine());
			// output = br.readLine();
			// }

			System.out.println("Testing where command syntax with no findings");
			String blueTeamDataDir = Paths.get("blue_team").toAbsolutePath().toString();
			OutputStreamWriterHelper.writeAndSend(bw, "where /r " + blueTeamDataDir + " *.pst");
			output = br.readLine();
			assertEquals("Attempting search with 10 minute timeout", output);
			output = br.readLine();
			assertEquals("Search complete with no findings", output);

			System.out.println("Testing where command syntax with known findings");
			OutputStreamWriterHelper.writeAndSend(bw, "where /r " + blueTeamDataDir + " *.md");
			output = br.readLine();
			assertEquals("Attempting search with 10 minute timeout", output);
			output = br.readLine();
			assertEquals(blueTeamDataDir + File.separator + "Bugs.md", output);
			output = br.readLine();
			assertEquals(blueTeamDataDir + File.separator + "IOC_Guide.md", output);
			output = br.readLine();
			assertEquals("", output);
			if (config.lang.equalsIgnoreCase("C++")) {
				assertEquals("", br.readLine());
			}
			output = br.readLine();
			assertEquals("Search complete", output);
		}
	}

	private static void testUplinkRandomBinaryFile(BufferedReader br, OutputStreamWriter bw, TestConfiguration config)
			throws IOException {
		if (!config.isRemote() && config.isExecInRoot()) {
			System.out.println("Testing uplink of random file");
			Random rnd = new Random();
			byte[] fileContent = new byte[24845];
			rnd.nextBytes(fileContent);
			String expectedFileBase64 = Base64.getEncoder().encodeToString(fileContent);
			Path tempFilePath = Paths.get("tmp_uplink_test");
			Files.write(tempFilePath, fileContent);
			OutputStreamWriterHelper.writeAndSend(bw, "uplink " + tempFilePath.toString());
			String output = br.readLine();
			assertEquals("<control> uplinked " + tempFilePath.toString() + " " + expectedFileBase64, output);
			Files.delete(tempFilePath);
		}
	}

	private static void testDownloadAndUplinkNominal(BufferedReader br, OutputStreamWriter bw, TestConfiguration config)
			throws IOException {
		// Test download and uplink as a pair
		System.out.println("Testing uplink and download - nominal case");
		OutputStreamWriterHelper.writeAndSend(bw,
				"<control> download test_uplink PFByb2plY3QgVG9vbHNWZXJzaW9uPSI0LjAiIHhtbG5zPSJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL2RldmVsb3Blci9tc2J1aWxkLzIwMDMiPgogIDxUYXJnZXQgTmFtZT0iSGVsbG8iPgogICAgPFNpbXBsZVRhc2sxIE15UHJvcGVydHk9IkhlbGxvISIgLz4KICA8L1RhcmdldD4KICA8VXNpbmdUYXNrCiAgICBUYXNrTmFtZT0iU2ltcGxlVGFzazEuU2ltcGxlVGFzazEiCiAgICBBc3NlbWJseUZpbGU9Im15X3Rhc2suZGxsIiAvPgo8L1Byb2plY3Q+");
		String output = br.readLine();
		assertEquals("File written: test_uplink", output);
		OutputStreamWriterHelper.writeAndSend(bw, "uplink test_uplink");
		output = br.readLine();
		assertEquals(
				"<control> uplinked test_uplink PFByb2plY3QgVG9vbHNWZXJzaW9uPSI0LjAiIHhtbG5zPSJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL2RldmVsb3Blci9tc2J1aWxkLzIwMDMiPgogIDxUYXJnZXQgTmFtZT0iSGVsbG8iPgogICAgPFNpbXBsZVRhc2sxIE15UHJvcGVydHk9IkhlbGxvISIgLz4KICA8L1RhcmdldD4KICA8VXNpbmdUYXNrCiAgICBUYXNrTmFtZT0iU2ltcGxlVGFzazEuU2ltcGxlVGFzazEiCiAgICBBc3NlbWJseUZpbGU9Im15X3Rhc2suZGxsIiAvPgo8L1Byb2plY3Q+", output);
		OutputStreamWriterHelper.writeAndSend(bw, "rm test_uplink");
		if(config.lang.equalsIgnoreCase("Java")) {
			br.readLine();//Flush line ending
		}
	}

	private static void testPwd(BufferedReader br, OutputStreamWriter bw, TestConfiguration config) {
		try {
			System.out.println("pwd test");
			bw.write("pwd" + System.lineSeparator());
			bw.flush();
			String output = br.readLine();
			if (config.os != TestConfiguration.OS.WINDOWS) {
				if (config.isRemote()) {
					assertEquals(TestConstants.EXECUTIONROOT_REMOTE + "/" + getRelativeRoot(config), output);
				} else {
					assertEquals(output, Paths.get("").toAbsolutePath().toString());
				}
			} else if (config.lang.equals("Native") || config.lang.equals("PowershellWindows")) {
				if (config.isRemote() || !config.isExecInRoot()) {
					assertTrue(output.startsWith("C:\\Users\\"));
				} else {
					assertEquals(output, Paths.get("").toAbsolutePath().toString());
				}
			} else {
				if (config.isExecInRoot()) {
					assertEquals(output, Paths.get("").toAbsolutePath().toString());
					// SMTP daemon generates files in working directory, and makes directory
					// enumeration unreliable
					if (!(config.lang.equals("python") && config.protocol.equals("SMTP"))) {
						System.out.println("dir test");
						testRootDirEnum(br, bw);
					}
				} else {
					if(config.lang.equals("C#")) {
						assertEquals(output, Paths.get("agents", "csc").toAbsolutePath().toString());
					}else {
						assertTrue(output.startsWith("C:\\Users\\"));
					}
					// Skip dir test. Since csc functionality for dir is tested in primary csc
					// daemon test, this is redundant
					// System.out.println("dir test");
					// testCscDirEnum(br, bw);
				}
			}

		} catch (IOException ex) {
			fail(ex.getMessage());
		}
	}

	private static void testClientIdentifesExecutable(BufferedReader br, OutputStreamWriter bw,
			TestConfiguration config) {
		System.out.println("Testing identifies executable");
		if (config.lang.equals("Native") || config.lang.equals("PowershellWindows")) {
			System.out.println("Native shell, cannot test client executable identification");
			return;
		}
		try {
			if (config.lang.equals("python")) {
				bw.write(Commands.CLIENT_CMD_GET_EXE + System.lineSeparator());
				bw.flush();
				String output = br.readLine();
				String[] respElements = output.split(" ");
				assertEquals(2, respElements.length);
				if (config.os == OS.WINDOWS) {
					assertTrue(respElements[0].startsWith("C:\\"));
					assertTrue(respElements[0].endsWith("python.exe"));
				} else {
					assertTrue(respElements[0].contains("python3"));
				}
				if (config.protocol.equals("DNS")) {
					assertEquals(Paths.get("agents", "python", "dnsSimpleAgent.py").toAbsolutePath().toString(),
							respElements[1]);
				} else if (config.protocol.equals("HTTPS")) {
					assertEquals(Paths.get("agents", "python", "httpsAgent.py").toAbsolutePath().toString(),
							respElements[1]);
				} else if (config.protocol.equals("SMTP")) {
					assertEquals(Paths.get("agents", "python", "emailAgent.py").toAbsolutePath().toString(),
							respElements[1]);
				}
			} else if (config.lang.equals("C#")) {
				bw.write(Commands.CLIENT_CMD_GET_EXE + System.lineSeparator());
				bw.flush();
				String output = br.readLine();
				assertTrue(output.startsWith("C:\\"));
				if (config.protocol.equals("HTTPS") || config.protocol.equals("SMB")) {
					if (config.isExecInRoot()) {
						assertTrue(output.endsWith("client_x.exe") || output.endsWith("stager_x.exe")
								|| output.endsWith("test_tmp.exe"));
					} else {
						assertTrue(output.toLowerCase().endsWith("msbuild.exe"));
					}
				} else if (config.protocol.equals("DNS")) {
					assertTrue(output.endsWith("dns_client_x.exe"));
				} else if (config.protocol.equals("SMTP")) {
					assertTrue(output.endsWith("EmailDaemon.exe"));
				} else {
					fail("Implement me");
				}
			} else if (config.lang.equals("C++")) {
				bw.write(Commands.CLIENT_CMD_GET_EXE + System.lineSeparator());
				bw.flush();
				String output = br.readLine();
				if (config.os != OS.WINDOWS) {
					assertTrue(output.startsWith("/"));
					if (config.protocol.equals("HTTPS")) {
						assertTrue(output.endsWith("http_daemon"));
					} else if (config.protocol.equals("DNS")) {
						assertTrue(output.endsWith("dns_daemon"));
					} else if (config.protocol.equals("SMTP")) {
						fail("Implement me");
					} else {
						fail("Implement me");
					}
				} else {
					assertTrue(output.startsWith("C:\\"));
					if (config.protocol.equals("HTTPS") || config.protocol.equals("SMB")) {
						assertTrue(output.endsWith("daemon.exe"));
					} else if (config.protocol.equals("DNS")) {
						assertTrue(output.endsWith("dns_daemon.exe"));
					} else if (config.protocol.equals("SMTP")) {
						assertTrue(output.endsWith("imap_daemon.exe"));
					} else {
						fail("Implement me");
					}
				}
			} else if (config.lang.equals("Native") || config.lang.equals("Java")) {
				fail("Implement me");
			}
		} catch (IOException ex) {
			fail(ex.getMessage());
		}
	}

	private static void testCatErrorHandling(BufferedReader br, OutputStreamWriter bw, TestConfiguration config) {
		if (config.lang.equals("Native") || config.lang.equals("PowershellWindows")) {
			// Native OS Windows just uses 'type' under the hood, Linux is pure cat
			// passthrough
			return;
		}
		try {
			//TODO Update this section - daemons should be able to handle multiple files
			System.out.println("Testing cat for too many arguments");
			bw.write("cat a_file arg b_file extraneous_input" + System.lineSeparator());
			bw.flush();
			String output = br.readLine();
			assertEquals(output, "No valid cat interpretation");

			System.out.println("Testing cat on nonexistent file");
			bw.write("cat a_file" + System.lineSeparator());
			bw.flush();
			output = br.readLine();
			assertEquals(output, "Invalid cat directive");

			System.out.println("Testing cat with a dumb flag");
			bw.write("cat -b execCentral.bat" + System.lineSeparator());
			bw.flush();
			output = br.readLine();
			assertEquals(output, "No valid cat interpretation");

			System.out.println("Testing cat >> with a nonexistent file");
			bw.write("cat no_file >> no_file2" + System.lineSeparator());
			bw.flush();
			output = br.readLine();
			assertEquals(output, "Invalid cat directive");
			
			System.out.println("Testing cat file X file2 with a bad operator");
			bw.write("cat execCentral.bat %% no_file" + System.lineSeparator());
			bw.flush();
			output = br.readLine();
			assertEquals(output, "No valid cat interpretation");
		} catch (IOException ex) {

		}
	}

	private static void testUplinkDownloadWithSpaces(BufferedReader br, OutputStreamWriter bw,
			TestConfiguration config) {
		try {
			if(!config.isExecInRoot()) {
				return;
			}
			System.out.println("Testing download with spaces");
			byte[] fileBytes = Files.readAllBytes(Paths.get("config", "test.properties"));
			byte[] encoded = Base64.getEncoder().encode(fileBytes);
			String encodedString = new String(encoded, StandardCharsets.US_ASCII);
			bw.write("<control> download test file " + encodedString + System.lineSeparator());
			bw.flush();
			String output = br.readLine();
			assertEquals(output, "File written: test file");
			if(!config.isRemote()) {
				assertTrue(Files.exists(Paths.get("test file")), "Test file not found");
				assertEquals(fileBytes.length, Files.readAllBytes(Paths.get("test file")).length, "Test file not correct");
			}

			System.out.println("Test uplink with spaces");
			bw.write("uplink test file" + System.lineSeparator());
			bw.flush();
			output = br.readLine();
			assertEquals(output, "<control> uplinked test file " + encodedString);

			if (config.os == OS.WINDOWS) {
				bw.write("del \"test file\"" + System.lineSeparator());
			} else {
				bw.write("rm 'test file'" + System.lineSeparator());
			}
			bw.flush();
			//TODO eliminate line flush from Python implementation
			if (!config.lang.equals("Native") && !config.lang.equals("PowershellWindows") && !config.lang.equals("C++")) {
				output = br.readLine();// Blank line
			}

		} catch (IOException ex) {
			ex.printStackTrace();
			fail(ex);
		}
	}

	private static void testUplinkDownloadErrorHandling(BufferedReader br, OutputStreamWriter bw) {
		try {
			System.out.println("Testing uplink for nonexistent file");
			bw.write("uplink a-fake-file" + System.lineSeparator());
			bw.flush();
			String output = br.readLine();
			assertEquals("Invalid uplink directive", output);

			System.out.println("Testing malformed download commands");
			// Forgetting to supply a filename
			bw.write("<control> download ASGSAOISJGSAGASG==" + System.lineSeparator());
			bw.flush();
			output = br.readLine();
			assertEquals("Invalid download directive", output);

			// Test bad b64 file
			System.out.println("Testing malformed download commands - B64");
			bw.write("<control> download fake_file A" + System.lineSeparator());
			bw.flush();
			output = br.readLine();
			assertEquals("Invalid download directive", output);

		} catch (IOException ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}

	private static void testWindowsNoExtraSpacesOnNativeExecutableOutput(BufferedReader br, OutputStreamWriter bw, TestConfiguration config) {
		if(config.os == OS.WINDOWS) {
		try {
			OutputStreamWriterHelper.writeAndSend(bw, WindowsFileSystemTraverser.getCommandForDriveLetterDiscovery());
			//This test makes sure that our output for native Windows executables is not messed up by bad formatting
			String output=br.readLine();
			assertTrue(output.startsWith("DeviceID"), "Something inserted before DeviceId: \"" + output + "\"");
			output=br.readLine();
			assertTrue(output.length() > 2 && output.charAt(1) == ':', "At least one drive latter should be returned, starting from the second line: '" + output + "'");
			output=br.readLine();
			while(output.length() > 2 && output.charAt(1) == ':') {
				output=br.readLine();
			}
			//Flush one extra line feed
			if(!config.lang.equals("Native")) {
				output=br.readLine();
			}
		}catch(Exception ex) {
			fail("Could not parse windows executable output" + ex.getMessage());
		}
		}
	}
	
	public static void cleanLogs() {
		Path logPath = Paths.get("test", "log");
		try {
			Files.walk(logPath).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		} catch (IOException e2) {
			// Will delete on next attempt
		}
	}

	public static void cleanup() {
		cleanLogs();
		File dir = new File("test");
		File[] matches = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				String hostname;
				try {
					hostname = InetAddress.getLocalHost().getHostName().toUpperCase();
					return name.toUpperCase().startsWith(hostname);
				} catch (UnknownHostException e) {
					return false;
				}
			}
		});

		if (matches.length >= 1) {

			try {
				Files.walk(matches[0].toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile)
						.forEach(File::delete);
			} catch (IOException e2) {
				e2.printStackTrace();
				fail("Cannot clean up harvester");
			}
		}

		try {
			// Delete Cookie Deleter tmp files
			Files.deleteIfExists(Paths.get(TestConstants.TMP_DIR, TestConstants.TMP_CHROME_COOKIES));
			Files.deleteIfExists(Paths.get(TestConstants.TMP_DIR, TestConstants.TMP_FIREFOX_COOKIES));
			Files.deleteIfExists(Paths.get(TestConstants.TMP_DIR, TestConstants.TMP_EDGE_COOKIES));

			// Delete Cookie stealer tmp files
			Files.deleteIfExists(Paths.get(TestConstants.TMP_DIR, TestConstants.TMP_GENERIC));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	static void testClipboard(BufferedReader br, OutputStreamWriter bw, TestConfiguration config)
			throws IOException {
		
		System.out.println("Testing clipboard");
		
		String clipboardContents = "This is the value of my clipboard";
		Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection sString = new StringSelection(clipboardContents);
		clip.setContents(sString, sString);
		
		bw.write("clipboard" + System.lineSeparator());
		bw.flush();
		String output = br.readLine();
		assertEquals(output, "Clipboard captured");

		//Give time for a write
		Time.sleepWrapped(1000);
		
		if (!config.isRemote()) {
			File dir = new File("test");

			File[] matches = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					String hostname;
					try {
						hostname = InetAddress.getLocalHost().getHostName();
						if (config.lang.equals("C++")) {
							hostname = hostname.toUpperCase();
						}
						// The file name will be hostname-pid to start
						if (config.lang.equalsIgnoreCase("Native") || config.lang.equals("PowershellWindows")) {
							return name.startsWith(hostname) && !name.startsWith(hostname + "-");
						} else {
							return name.startsWith(hostname) && name.matches(".*\\d.*");
						}
					} catch (UnknownHostException e) {
						return false;
					}
				}
			});
			assertTrue(matches.length > 0, "No directories found for hostname");
			File[] clipboard = matches[0].listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.startsWith("Clipboard");
				}
			});
			assertEquals(1, clipboard.length);
			String data = Files.readString(clipboard[0].toPath());
			if(config.lang.equals("C#")) {
				//Bug in C# library causes clipboard content not to return sometimes. Either returns nothing or correct
				if(data.length() != 0) {
					assertEquals(clipboardContents + System.lineSeparator(), data);
				}
			}else if(config.lang.equals("C++") && config.protocol.equals("DNS")) {
				assertEquals(clipboardContents, data);
			}else {
				assertEquals(clipboardContents + System.lineSeparator(), data);
			}

			try {
				Files.walk(matches[0].toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile)
						.forEach(File::delete);
			} catch (IOException e2) {
				e2.printStackTrace();
				fail("Cannot clean up harvester");
			}
		}
	}
	
	static void testCpMv(BufferedReader br, OutputStreamWriter bw, TestConfiguration config) throws IOException {
		//If this doesn't work, it can be validated in a local test. Or someone can enhance it for remote testing
		if(!config.isRemote()) {
			System.out.println("Testing cp and mv");
			OutputStreamWriterHelper.writeAndSend(bw, "cat >narf.txt");
			OutputStreamWriterHelper.writeAndSend(bw, "test");
			OutputStreamWriterHelper.writeAndSend(bw, "<done>");
			assertEquals("Data written", br.readLine(), "Did not get positive response for writing sample data, cp and mv test");
			assertTrue(Files.exists(Paths.get("narf.txt")), "Narf is not written");
		
			OutputStreamWriterHelper.writeAndSend(bw, "mv narf.txt barf.txt");
			Time.sleepWrapped(2500);//mv provides no return data
			assertFalse(Files.exists(Paths.get("narf.txt")), "Original test file still there");
			assertTrue(Files.exists(Paths.get("barf.txt")), "Renamed test file not present");
			if(config.lang.equals("Native") && config.os == OS.WINDOWS) {
				assertTrue(br.readLine().contains("1 file(s) moved"), "Windows shell 'MOVE' not returned");
			}
			
			OutputStreamWriterHelper.writeAndSend(bw, "cp barf.txt narf.txt");
			Time.sleepWrapped(2500);//mv provides no return data
			assertTrue(Files.exists(Paths.get("narf.txt")), "Copied file not present");
			assertTrue(Files.exists(Paths.get("barf.txt")), "Original file for copy operation not present");
			if(config.lang.equals("Native") && config.os == OS.WINDOWS) {
				assertTrue(br.readLine().contains("1 file(s) copied"), "Windows shell 'MOVE' not returned");
			}
			
			assertDoesNotThrow(() -> Files.delete(Paths.get("narf.txt")));
			assertDoesNotThrow(() -> Files.delete(Paths.get("barf.txt")));
		}
	}
	
	static void testRmDel(BufferedReader br, OutputStreamWriter bw, TestConfiguration config) throws IOException {
		System.out.println("Testing del and rm");
		OutputStreamWriterHelper.writeAndSend(bw, "cat >narf.txt");
		OutputStreamWriterHelper.writeAndSend(bw, "test");
		OutputStreamWriterHelper.writeAndSend(bw, "<done>");
		assertEquals("Data written", br.readLine());
		assertTrue(Files.exists(Paths.get("narf.txt")), "Narf is not written");
		OutputStreamWriterHelper.writeAndSend(bw, "rm narf.txt");
		
		int idx = 0;
		//Wait for 30 seconds max
		while((idx < 300) && Files.exists(Paths.get("narf.txt"))){
			Time.sleepWrapped(100);//rm provides no return data
			idx++;
		}	
		
		assertFalse(Files.exists(Paths.get("narf.txt")), "Narf is not deleted");
		OutputStreamWriterHelper.writeAndSend(bw, "cat >narf.txt");
		OutputStreamWriterHelper.writeAndSend(bw, "test");
		OutputStreamWriterHelper.writeAndSend(bw, "<done>");
		assertEquals("Data written", br.readLine());
		assertTrue(Files.exists(Paths.get("narf.txt")), "Narf is not written");
		OutputStreamWriterHelper.writeAndSend(bw, "del narf.txt");
		idx = 0;
		//Wait for 30 seconds max
		while((idx < 300) && Files.exists(Paths.get("narf.txt"))){
			Time.sleepWrapped(100);//rm provides no return data
			idx++;
		}	
		assertFalse(Files.exists(Paths.get("narf.txt")), "Narf is not deleted");
		
		//Native shells don't always reflect std:err
		if(!config.lang.equals("Native")) {
			OutputStreamWriterHelper.writeAndSend(bw, "del i_dont_exist");
			String output = br.readLine();
			assertTrue(output.contains("Error") || output.contains("error") || output.contains("Could Not Find") || output.contains("File does not exist, cannot remove"), "Did not receive correct file deletion error message");
			OutputStreamWriterHelper.writeAndSend(bw, "rm i_dont_exist");
			output = br.readLine();
			assertTrue(output.contains("Error") || output.contains("error") || output.contains("Could Not Find") || output.contains("File does not exist, cannot remove"), "Did not receive correct file deletion error message");
		}
	}
	
	static void testLs(BufferedReader br, OutputStreamWriter bw, TestConfiguration config) throws IOException {
		if(!config.lang.equals("Native") && !config.lang.equals("PowershellNative")) {
			System.out.println("Testing standardized ls");
			DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			//OutputStreamWriterHelper.writeAndSend(bw, "cd agents");
			//br.readLine();//Flush pwd return
			OutputStreamWriterHelper.writeAndSend(bw, "ls config");
			String headerStr = br.readLine().replaceAll("\\s+", " ");
			String[] headers = headerStr.split(" ");
			assertEquals(6, headers.length, "Incorrect header length: '" + headerStr + "'");
			assertEquals("Mode", headers[0]);
			assertEquals("Last", headers[1]);
			assertEquals("Write", headers[2]);
			assertEquals("Time", headers[3]);
			assertEquals("Length", headers[4]);
			assertEquals("Name", headers[5]);
			
			boolean moreLinesExist = true;
			while(moreLinesExist) {
				String nextLine = br.readLine().replaceAll("\\s+", " ");
				if(nextLine.equals("")) {
					moreLinesExist = false;
				}else {
					String[] contents = nextLine.split(" ");
					if(contents[0].equals("d")) {
						assertEquals(4, contents.length, "Did not observe 4 elements in: '" + nextLine + "'");
					}else {
						assertEquals(5, contents.length, "Did not observe 5 elements in: '" + nextLine + "'");
						assertDoesNotThrow(() -> Integer.parseInt(contents[3]));
					}
					assertTrue(contents[0].equals("d") || contents[0].equals("-"));
					assertDoesNotThrow(() -> f.parse(contents[1] + " " + contents[2]));
				}
			}
			
		}
	}
	
	static void testMkdirRmdir(BufferedReader br, OutputStreamWriter bw, TestConfiguration config) throws IOException {
		System.out.println("Testing rm/mk dir");
		OutputStreamWriterHelper.writeAndSend(bw, "mkdir narf");
		
		int idx = 0;
		//Wait for 30 seconds max
		while((idx < 300) && !Files.exists(Paths.get("narf"))){
			Time.sleepWrapped(100);//mkdir provides no return data
			idx++;
		}
		
		assertTrue(Files.exists(Paths.get("narf")), "Test dir not created");
		if(!config.lang.equals("Native")) {
			OutputStreamWriterHelper.writeAndSend(bw, "mkdir narf");
			String output = br.readLine();
			assertTrue(output.contains("error") || output.contains("Error") || output.contains("already exists") || output.contains("Unable to create directory"), "Did not receive error message from mkdir command");
		}
		OutputStreamWriterHelper.writeAndSend(bw, "rmdir narf");
		
		idx = 0;
		//Wait for 30 seconds max
		while((idx < 300) && Files.exists(Paths.get("narf"))){
			Time.sleepWrapped(100);//rkdir provides no return data
			idx++;
		}
		
		assertFalse(Files.exists(Paths.get("narf")), "Test dir not removed");
		if(!config.lang.equals("Native")) {
			OutputStreamWriterHelper.writeAndSend(bw, "rmdir narf");
			String output = br.readLine();
			assertTrue(output.contains("error") || output.contains("Error") || output.contains("cannot find") || output.contains("Directory does not exist, cannot remove"), "Did not receive error message from rmdir command");
		}
	}

	static void testCat(BufferedReader br, OutputStreamWriter bw, TestConfiguration config) throws IOException {
		// Test simple CAT reading a file
		System.out.println("General cat test - reading file");
		String targetFileRoot = "execCentral.bat";
		String targetTempCopyRoot = "execCentral.bat.tmp";
		String targetFile = targetFileRoot;
		String targetTempCopy = targetTempCopyRoot;
		if (!config.isExecInRoot()) {
			targetFile = "..\\..\\execCentral.bat";
			targetTempCopy = "..\\..\\execCentral.bat.tmp";
		}
		bw.write("cat " + targetFile + System.lineSeparator());
		bw.flush();
		String output = br.readLine();
		String actualCatFileContents = new String(Files.readAllBytes(Paths.get(targetFile)));
		assertEquals(output, actualCatFileContents);
		if (!config.lang.equals("Java") && !((config.lang.equals("Native") || config.lang.equals("PowershellWindows")) && config.os == OS.WINDOWS)) {
			System.out.println("reading flush");
			output = br.readLine();
			if (config.lang.equals("Native") && config.os == TestConfiguration.OS.WINDOWS) {
				assertTrue(output.startsWith("C:\\"));
			} else {
				assertEquals(output, "");
			}
		}

		if (!(config.lang.equals("Native") || config.lang.equals("PowershellWindows")) || config.os == TestConfiguration.OS.LINUX) {
			// Test simple CAT reading a file with line numbers
			System.out.println("Cat test with line numbers");
			bw.write("cat -n " + targetFile + System.lineSeparator());
			bw.flush();
			output = br.readLine();
			if (config.os == TestConfiguration.OS.LINUX && config.lang.equals("Native")) {
				assertEquals(output, "     1\t" + actualCatFileContents);
			} else {
				assertEquals(output, "1: " + actualCatFileContents);
			}
			if (!config.lang.equals("Java")) {
				output = br.readLine();
				assertEquals(output, "");
			}
		}

		// Test CAT writing to new file
		if (((config.lang.equals("Native") || config.lang.equals("PowershellWindows") || config.lang.equals("C++") || config.lang.equals("python") || config.lang.equals("Java")) && !config.protocol.equals("SMB"))
				) {// || isLinux) {
			if (config.isExecInRoot()) {
				bw.write("cat >newFile.txt" + System.lineSeparator());
			} else {
				bw.write("cat >..\\..\\newFile.txt" + System.lineSeparator());
			}
			bw.flush();
			bw.write("Line" + System.lineSeparator());
			bw.flush();
			bw.write("otherline and stuff" + System.lineSeparator());
			bw.flush();
			bw.write("<done>" + System.lineSeparator());
			bw.flush();

			// Minimum beacon time
			try {
				//TODO Evaluate if this beacon time can be deprecated
				Thread.sleep(2500);
			} catch (InterruptedException ex) {
				// ignore
			}

			System.out.println("Checking for write confirmation");
			output = br.readLine();
			assertEquals(output, "Data written");

			if (!config.isRemote()) {
				BufferedReader fileReader = new BufferedReader(new FileReader("newFile.txt"));
				;
				String line = fileReader.readLine();
				assertEquals(line, "Line");
				line = fileReader.readLine();
				assertEquals(line, "otherline and stuff");
				line = fileReader.readLine();
				assertEquals(line, null);
				fileReader.close();
			} else {
				bw.write("cat newFile.txt" + System.lineSeparator());
				bw.flush();
				output = br.readLine();
				assertEquals(output, "Line");
				output = br.readLine();
				assertEquals(output, "otherline and stuff");
				output = br.readLine();
				assertEquals(output, "");
				if (config.lang.equals("Native")) {
					output = br.readLine();
					if (config.isRemote() && config.os == OS.WINDOWS) {
						assertTrue(output.startsWith("C:\\Users\\"));
					} else {
						assertEquals(output, "");
					}

				}
			}

			// Test CAT canceling writing to a new file
			System.out.println("Testing cat cancellation");
			if (config.isExecInRoot()) {
				bw.write("cat >newFile.txt" + System.lineSeparator());
			} else {
				bw.write("cat >..\\..\\newFile.txt" + System.lineSeparator());
			}
			bw.write("Line" + System.lineSeparator());
			bw.write("otherline and stuff" + System.lineSeparator());
			bw.write("<cancel>" + System.lineSeparator());
			bw.flush();

			output = br.readLine();
			assertEquals(output, "Abort: No file write");

			System.out.println("Test CAT appending to an existing file");
			if (config.isExecInRoot()) {
				bw.write("cat >>newFile.txt" + System.lineSeparator());
			} else {
				bw.write("cat >>..\\..\\newFile.txt" + System.lineSeparator());
			}
			bw.flush();
			bw.write("Line" + System.lineSeparator());
			bw.flush();
			bw.write("otherline and stuff" + System.lineSeparator());
			bw.flush();
			bw.write("<done>" + System.lineSeparator());
			bw.flush();

			System.out.println("Checking for write confirmation");
			output = br.readLine();
			assertEquals(output, "Data written");

			if (config.isRemote()) {
				bw.write("cat newFile.txt" + System.lineSeparator());
				bw.flush();
				output = br.readLine();
				assertEquals(output, "Line");
				output = br.readLine();
				assertEquals(output, "otherline and stuff");
				output = br.readLine();
				assertEquals(output, "Line");
				output = br.readLine();
				assertEquals(output, "otherline and stuff");
				output = br.readLine();
				assertEquals(output, "");
				// output = br.readLine();
				// assertEquals(output, "");

				if (config.os != TestConfiguration.OS.LINUX) {
					bw.write("del newFile.txt" + System.lineSeparator());
					bw.flush();
					output = br.readLine();
					assertTrue(output.startsWith("C:\\Users\\"));
					output = br.readLine();// newline flush
					output = br.readLine();// prompt flush
				} else {
					bw.write("rm newFile.txt" + System.lineSeparator());
					bw.flush();
					output = br.readLine();
					assertEquals(output, "");
				}
			} else {
				BufferedReader fileReader = new BufferedReader(new FileReader("newFile.txt"));
				;
				String line = fileReader.readLine();
				assertEquals(line, "Line");
				line = fileReader.readLine();
				assertEquals(line, "otherline and stuff");
				line = fileReader.readLine();
				assertEquals(line, "Line");
				line = fileReader.readLine();
				assertEquals(line, "otherline and stuff");
				line = fileReader.readLine();
				assertEquals(line, null);
				fileReader.close();
				Files.deleteIfExists(Paths.get("newFile.txt"));
			}
		}

		// Test CAT copying to file
		System.out.println("Test cat copying file");
		bw.write("cat " + targetFile + " > " + targetTempCopy + System.lineSeparator());
		bw.flush();
		// Minimum beacon time
		try {
			Thread.sleep(2500);
		} catch (InterruptedException ex) {
			// ignore
		}
		output = br.readLine();
		assertEquals(output, "File write executed");
		// We will test in the >> use case that this file write occured
		byte[] f1;
		byte[] f2;
		if (!config.isRemote()) {
			f1 = Files.readAllBytes(Paths.get(targetFileRoot));
			f2 = Files.readAllBytes(Paths.get(targetTempCopyRoot));
			assertTrue(Arrays.equals(f1, f2), "Test copied files are not equal: reference " + f1.length + " vs " + f2.length);
		}
		// Test CAT appending to an existing file from existing file
		System.out.println("Test cat copying appended file");
		bw.write("cat " + targetFile + " >> " + targetTempCopy + System.lineSeparator());
		bw.flush();
		
		output = br.readLine();
		assertEquals(output, "Appended file");
		if (config.isRemote()) {
			bw.write("uplink execCentral.bat.tmp" + System.lineSeparator());
			bw.flush();
			output = br.readLine();
			assertEquals(output,
					"<control> uplinked execCentral.bat.tmp amF2YSAtY3AgInRhcmdldC8qO3RhcmdldC9saWIvKiIgYzIuUnVubmVyIGNvbmZpZy90ZXN0LnByb3BlcnRpZXNqYXZhIC1jcCAidGFyZ2V0Lyo7dGFyZ2V0L2xpYi8qIiBjMi5SdW5uZXIgY29uZmlnL3Rlc3QucHJvcGVydGllcw==");
			bw.write("rm execCentral.bat.tmp" + System.lineSeparator());
			bw.flush();
			
			if(config.lang.equals("C++")) {
				output = br.readLine();
				assertEquals("", output);
			}

			System.out.println("Cleaned up cat test");
		} else {
			f1 = Files.readAllBytes(Paths.get(targetFileRoot));
			f2 = Files.readAllBytes(Paths.get(targetTempCopyRoot));
			assertTrue(f1.length * 2 == f2.length, "Test files are not equal");
		}

		Files.deleteIfExists(Paths.get(targetTempCopyRoot));
	}

	static void testShell(BufferedReader br, OutputStreamWriter bw, TestConfiguration config) throws IOException {
		bw.write("shell_list" + System.lineSeparator());
		bw.flush();
		String response = br.readLine();
		assertEquals("No shells active", response);

		bw.write("shell" + System.lineSeparator());
		bw.write("shell_background" + System.lineSeparator());
		bw.write("shell_list" + System.lineSeparator());
		bw.flush();
		response = br.readLine();
		assertEquals("Shell Launched: 0", response);
		response = br.readLine();
		assertEquals("Proceeding in main shell", response);
		response = br.readLine();
		assertEquals("Sessions available: ", response);
		response = br.readLine();
		assertEquals("Shell 0: No Process", response);
		response = br.readLine();
		assertEquals("", response);

		bw.write("shell" + System.lineSeparator());
		bw.write("shell_background" + System.lineSeparator());
		bw.write("shell_list" + System.lineSeparator());
		bw.flush();
		response = br.readLine();
		assertEquals("Shell Launched: 1", response);
		response = br.readLine();
		assertEquals("Proceeding in main shell", response);
		response = br.readLine();
		assertEquals("Sessions available: ", response);
		response = br.readLine();
		assertEquals("Shell 0: No Process", response);
		response = br.readLine();
		assertEquals("Shell 1: No Process", response);
		response = br.readLine();
		assertEquals("", response);

		// Test program has a simple loop. Asks the user "enter a string", and responds
		// with the entry.
		// Enter "crash" to cause a segfault. Enter "quit" to quit gracefully.

		bw.write("shell 0" + System.lineSeparator());
		if (config.os == OS.LINUX) {
			if (config.isRemote()) {
				bw.write("python3 " + TestConstants.EXECUTIONROOT_REMOTE
						+ "/TheAllCommander/test_support_scripts/basic_io_loop.py" + System.lineSeparator());
			} else {
				bw.write("python3 " + Paths.get("").toAbsolutePath().toString() + "/basic_io_loop.py"
						+ System.lineSeparator());
			}
		} else {
			bw.write("python test_support_scripts" + System.getProperty("file.separator")
					+ System.getProperty("file.separator") + "basic_io_loop.py" + System.lineSeparator());
		}
		bw.write("test_io" + System.lineSeparator());
		bw.flush();
		Time.sleepWrapped(7500);// Let the other process do its thing
		bw.write("shell_background" + System.lineSeparator());
		bw.write("shell_list" + System.lineSeparator());
		bw.flush();
		response = br.readLine();
		assertEquals("Active Session: 0", response);
		response = br.readLine();
		assertEquals("Enter a String:", response);
		response = br.readLine();
		if (!config.lang.equals("C++")) {
			assertEquals("", response);
			response = br.readLine();
			assertEquals("You entered: test_io", response);
		} else {
			// C++ may or may not place a newline, so we test if we have a blank link before
			// moving forward
			if (!response.equals("You entered: test_io")) {
				response = br.readLine();
				assertEquals("You entered: test_io", response);
			}
		}
		response = br.readLine();
		assertEquals("Enter a String:", response);
		response = br.readLine();
		if (!config.lang.equals("C++")) {
			assertEquals("", response);
			response = br.readLine();
			assertEquals("Proceeding in main shell", response);
		} else {
			// C++ may or may not place a newline, so we test if we have a blank link before
			// moving forward
			if (!response.equals("Proceeding in main shell")) {
				response = br.readLine();
				assertEquals("Proceeding in main shell", response);
			}
		}
		response = br.readLine();
		assertEquals("Sessions available: ", response);
		response = br.readLine();
		if (config.os == OS.LINUX) {
			if (config.isRemote()) {
				assertEquals("Shell 0: python3 " + TestConstants.EXECUTIONROOT_REMOTE
						+ "/TheAllCommander/test_support_scripts/basic_io_loop.py", response);
			} else {
				assertEquals("Shell 0: python3 " + Paths.get("").toAbsolutePath().toString() + "/basic_io_loop.py",
						response);
			}
		} else {
			assertEquals("Shell 0: python test_support_scripts" + System.getProperty("file.separator")
					+ System.getProperty("file.separator") + "basic_io_loop.py", response);
		}
		response = br.readLine();
		assertEquals("Shell 1: No Process", response);
		response = br.readLine();
		assertEquals("", response);

		System.out.println("Testing crash");
		bw.write("shell 0" + System.lineSeparator());
		bw.write("crash" + System.lineSeparator());
		bw.write("shell_background" + System.lineSeparator());
		bw.write("shell_list" + System.lineSeparator());
		bw.flush();
		response = br.readLine();
		assertEquals("Active Session: 0", response);
		response = br.readLine();
		assertEquals("Proceeding in main shell", response);
		response = br.readLine();
		assertEquals("Sessions available: ", response);
		response = br.readLine();
		if (config.os == OS.LINUX) {
			if (config.isRemote()) {
				assertEquals(
						"Shell 0: python3 " + TestConstants.EXECUTIONROOT_REMOTE
								+ "/TheAllCommander/test_support_scripts/basic_io_loop.py exited with code 139",
						response);
			} else {
				assertEquals("Shell 0: python3 " + Paths.get("").toAbsolutePath().toString()
						+ "/basic_io_loop.py exited with code 139", response);
			}
		} else {
			assertEquals("Shell 0: python test_support_scripts" + System.getProperty("file.separator")
					+ System.getProperty("file.separator") + "basic_io_loop.py exited with code 139", response);
		}
		response = br.readLine();
		assertEquals("Shell 1: No Process", response);
		response = br.readLine();
		assertEquals("", response);

		bw.write("shell_kill 1" + System.lineSeparator());
		bw.write("shell_list" + System.lineSeparator());
		bw.flush();
		response = br.readLine();
		assertEquals("Session destroyed: 1", response);
		response = br.readLine();
		assertEquals("Sessions available: ", response);
		response = br.readLine();
		if (config.os == OS.LINUX) {
			if (config.isRemote()) {
				assertEquals(
						"Shell 0: python3 " + TestConstants.EXECUTIONROOT_REMOTE
								+ "/TheAllCommander/test_support_scripts/basic_io_loop.py exited with code 139",
						response);
			} else {
				assertEquals("Shell 0: python3 " + Paths.get("").toAbsolutePath().toString()
						+ "/basic_io_loop.py exited with code 139", response);
			}
		} else {
			assertEquals("Shell 0: python test_support_scripts" + System.getProperty("file.separator")
					+ System.getProperty("file.separator") + "basic_io_loop.py exited with code 139", response);
		}
		response = br.readLine();
		assertEquals("", response);

		bw.write("shell 0" + System.lineSeparator());
		bw.write("shell_kill" + System.lineSeparator());
		bw.write("shell_list" + System.lineSeparator());
		bw.flush();
		response = br.readLine();
		assertEquals("Active Session: 0", response);
		response = br.readLine();
		assertEquals("Session Destroyed", response);
		response = br.readLine();
		assertEquals("No shells active", response);
	}
}
