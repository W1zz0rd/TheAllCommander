package c2.python;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import c2.smtp.EmailHandler;
import c2.smtp.SimpleEmail;
import util.test.ClientServerTest;
import util.test.RunnerTestGeneric;
import util.test.TestConfiguration;
import util.test.TestConstants;

public class RunnerTestPythonEmail  extends ClientServerTest {

	@Test
	void test() {
		testEmail();
	}

	static Properties setup() {
		try (InputStream input = new FileInputStream("config" + File.separator + "test.properties")) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			return prop;
		} catch (IOException ex) {
			System.out.println("Unable to load config file");
			fail(ex.getMessage());
			return null;
		}
	}
	
	public static void testEmail() {
		System.out.println("Warning: The email based protocol is currently partially lossy, and will often not make it through the full automated sequence");
		flushC2Emails();
		initiateServer();
		String clientCmd = "cmd /c \"start " + TestConstants.PYTHON_EXE + " agents" + File.separator + "python" + File.separator + "emailAgent.py\"";
		spawnClient(clientCmd);
		
		TestConfiguration testConfig = new TestConfiguration(TestConfiguration.OS.WINDOWS, "python", "SMTP");
		RunnerTestGeneric.test(testConfig);
		
		teardown();
	}
	
	public static void flushC2Emails() {
		EmailHandler receiveHandler = new EmailHandler();
		try {
			receiveHandler.initialize(null, setup(), null, null);
		} catch (Exception ex) {
			fail(ex.getMessage());
		}
		
		SimpleEmail email = receiveHandler.getNextMessage();
		while(email != null) {
			email = receiveHandler.getNextMessage();
		}
	}
}
