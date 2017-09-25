package org.anichakra.tools.db;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.anichakra.tools.db.rdbms.RdbmsPerfinder;

public class Application {
	private final static short EXECUTION_COUNT = 3;

	public static void main(String[] argv) throws Exception {
		// Get and validate input from System properties
		String jdbcDriverClassName = System.getProperty("jdbc.driver");		
		String jarPath = System.getProperty("jdbc.jarPath");		

		String connectionUrl = System.getProperty("jdbc.url");
		String username = System.getProperty("jdbc.username");
		String password = System.getProperty("jdbc.password");
		String query = System.getProperty("jdbc.query");
		String[] parameters = Optional.ofNullable(System.getProperty("jdbc.parameters")).map(s->s.split(",")).orElse(null);
		Integer fetchSize = getIntegerValue(System.getProperty("jdbc.fetchSize"));
		Integer rowIndex = getIntegerValue(System.getProperty("jdbc.rowIndex"));
		Integer maxRows = getIntegerValue(System.getProperty("jdbc.maxRows"));

		try (RdbmsPerfinder rdbmsPf = new RdbmsPerfinder();) {
			System.out.println("Loading JDBC Driver: " + jdbcDriverClassName);
			rdbmsPf.loadDriver(jdbcDriverClassName, jarPath);
			System.out.println("Creating Connection to: " + connectionUrl);
			rdbmsPf.createConnection(connectionUrl, username, password);
			System.out.println("Executing query: " + query);
			rdbmsPf.prepareStatement(query, fetchSize, parameters);
			rdbmsPf.executeQuery(); // warm up
			List<?> result = rdbmsPf.fetchResult(rowIndex, maxRows); // dry run
			System.out.println("Count:" + result.size());
			System.out.println("Records:" + result);
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
			System.out.println("Query Times (ms):" + Arrays.toString(queryTimes));
			System.out.println("Fetch Times (ms):" + Arrays.toString(fetchTimes));
			System.out.println("Average Query Time (ms):" + Arrays.stream(queryTimes).average().getAsDouble());
			System.out.println("Average Fetch Time (ms):" + Arrays.stream(fetchTimes).average().getAsDouble());

		}
	}

	private static Integer getIntegerValue(String property) {
		if (property == null || property.trim().length() == 0)
			return null;
		return Integer.valueOf(property);
	}

}
