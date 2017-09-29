package org.anichakra.tools.db.perfinder;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.anichakra.tools.db.perfinder.rdbms.RdbmsPerfinder;

public class Application {
	private final static short EXECUTION_COUNT = 3;

	private static Integer getIntegerValue(String property) {
		if (property == null || property.length() == 0)
			return null;
		return Integer.valueOf(property.trim());
	}

	private static String getPasswordFromCommand() {
		Console console = System.console();
		if (console == null) {
			throw new IllegalArgumentException("jdbc.password is not found in input or configuration!");
		}
		char[] passwordArray = console.readPassword("Enter jdbc.password: ");
		return new String(passwordArray);

	}

	public static void main(String[] argv) throws Exception {
		// Get and validate input from System properties
		String propertiesFile = System.getProperty("jdbc.properties");
		String outputFile = System.getProperty("result.out");
		PrintStream out = null;
		if (outputFile == null) {
			out = System.out;
		} else {
			out = new PrintStream(new File(outputFile));
		}

		Properties jdbcProperties = new Properties();
		if (propertiesFile != null) {
			try (FileInputStream in = new FileInputStream(propertiesFile);) {
				jdbcProperties.load(in);
			}
		}
		String jdbcDriverClassName = jdbcProperties.getProperty("jdbc.driver");
		String jarPath = jdbcProperties.getProperty("jdbc.jarPath");

		String connectionUrl = jdbcProperties.getProperty("jdbc.url");
		String username = jdbcProperties.getProperty("jdbc.username");
		String password = jdbcProperties.getProperty("jdbc.password");
		if ((username != null && username.length() > 0) && (password == null || password.length() == 0)) {
			password = getPasswordFromCommand();
		}
		String query = jdbcProperties.getProperty("jdbc.query");
		if (query == null || query.trim().length() == 0) {
			String queryFile = jdbcProperties.getProperty("jdbc.queryFile");
			Path path = Paths.get(queryFile);
			StringBuilder data = new StringBuilder();
			try (Stream<String> lines = Files.lines(path);) {
				lines.forEach(line -> data.append(line).append("\n"));
				query = data.toString();
			}
		}

		String[] parameters = Optional.ofNullable(jdbcProperties.getProperty("jdbc.parameters")).map(s -> s.split(","))
				.orElse(null);
		Integer fetchSize = getIntegerValue(jdbcProperties.getProperty("jdbc.fetchSize"));
		Integer rowIndex = getIntegerValue(jdbcProperties.getProperty("jdbc.rowIndex"));
		Integer maxRows = getIntegerValue(jdbcProperties.getProperty("jdbc.maxRows"));

		try (RdbmsPerfinder rdbmsPf = new RdbmsPerfinder();) {
			System.out.println("Loading JDBC Driver: " + jdbcDriverClassName);
			rdbmsPf.loadDriver(jdbcDriverClassName, jarPath);
			System.out.println("Creating Connection to: " + connectionUrl);
			rdbmsPf.createConnection(connectionUrl, username, password);
			out.println("Executing Query: " + query);
			rdbmsPf.prepareStatement(query, fetchSize, parameters);
			rdbmsPf.executeQuery(); // warm up
			List<?> result = rdbmsPf.fetchResult(rowIndex, maxRows); // dry run
			out.println("Record Count:" + result.size());
			out.println("Data:");
			out.println(result);
			System.out.println("Result...");
			long[] queryTimes = new long[EXECUTION_COUNT];
			long[] fetchTimes = new long[EXECUTION_COUNT];

			for (int i = 0; i < EXECUTION_COUNT; i++) {
				long time = System.currentTimeMillis();
				rdbmsPf.executeQuery();
				queryTimes[i] = System.currentTimeMillis() - time;
				time = System.currentTimeMillis();
				rdbmsPf.fetchResult(rowIndex, maxRows);
				fetchTimes[i] = System.currentTimeMillis() - time;
			}
			System.out.println("Result is published below:");
			System.out.println("Query Times (ms):" + Arrays.toString(queryTimes));
			System.out.println("Fetch Times (ms):" + Arrays.toString(fetchTimes));
			System.out.println("Average Query Time (ms):" + Arrays.stream(queryTimes).average().getAsDouble());
			System.out.println("Average Fetch Time (ms):" + Arrays.stream(fetchTimes).average().getAsDouble());
			if (outputFile != null) {
				out.println("Query Times (ms):" + Arrays.toString(queryTimes));
				out.println("Fetch Times (ms):" + Arrays.toString(fetchTimes));
				out.println("Average Query Time (ms):" + Arrays.stream(queryTimes).average().getAsDouble());
				out.println("Average Fetch Time (ms):" + Arrays.stream(fetchTimes).average().getAsDouble());
			}

		}
	}

}
