package c2;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import c2.nativeshell.RunnerTestNativeLinuxDaemon;
import c2.python.RunnerTestKeyloggerDNS;
import c2.python.RunnerTestKeyloggerEmail;
import c2.python.RunnerTestKeyloggerHTTPS;
import c2.python.RunnerTestPython;
import c2.python.RunnerTestPythonEmail;
import c2.win.RunnerTestPythonHTTPSDaemonWinRDP;

class MasterLinearTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test() {
		//Test Keyloggers
		RunnerTestKeyloggerHTTPS.test();
		RunnerTestKeyloggerDNS.test();
		RunnerTestKeyloggerEmail.test();
		
		//Testing Linux
		System.out.println("Testing Linux Native Shell");
		RunnerTestNativeLinuxDaemon.test();
		
		//Python
		System.out.println("Testing Python");
		RunnerTestPythonEmail.testEmail();
		
		System.out.println("Testing Python multiple sessions");
		RunnerTestPython.testHTTPSTwoSessions();
		RunnerTestPython.testDNSTwoSessions();

		RunnerTestPythonHTTPSDaemonWinRDP.test();
	}

}
