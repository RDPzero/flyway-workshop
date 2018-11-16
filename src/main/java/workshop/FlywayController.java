package workshop;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FlywayController {

	private static final Logger logger = LoggerFactory.getLogger(FlywayController.class);

	@Autowired
	private DataSource dataSource;

	@GetMapping("/flyway/repair")
	public String repair(final Flyway flyway, @RequestParam(value = "outOfOrder", defaultValue = "false") Boolean outOfOrder) {
		flyway.setDataSource(dataSource);
		logger.warn("Repairing flyway...");
		flyway.repair();
		logger.warn("Repair completed! Migrating (outOfOrder: {})...", outOfOrder);
		flyway.setOutOfOrder(outOfOrder.booleanValue());
		flyway.migrate();
		logger.warn("Migration completed!");

		return info(flyway);
	}

	@GetMapping("/flyway/info")
	public String info(final Flyway flyway) {
		flyway.setDataSource(dataSource);
		logger.warn("Info for flyway...");
		MigrationInfoService info = flyway.info();

		MigrationInfo current = info.current();
		StringBuilder infos = new StringBuilder();

		if (current == null) {
			logger.info("=> No migrations have been run yet.");
			infos.append("\n=> No migrations have been run yet.\n");
		} else {
			logger.info("=> Currently applied migration:");
			infos.append("\n=> Currently applied migration:\n");
			logger.info(getLogHeader());
			infos.append(getLogHeader() + "\n");
			logger.info(getLogLine(current));
			infos.append(getLogLine(current) + "\n");
		}

		if (info.applied().length > 0) {
			logger.info("=> Applied migrations (already on database):");
			infos.append("\n=> Applied migrations (already on database):\n");
			logger.info(getLogHeader());
			infos.append(getLogHeader() + "\n");
			for (MigrationInfo migration : info.applied()) {
				logger.info(getLogLine(migration));
				infos.append(getLogLine(migration) + "\n");
			}
		} else {
			logger.info("=> No applied migrations");
			infos.append("\n=> No applied migrations\n");
		}

		if (info.pending().length > 0) {
			logger.info("=> Pending migrations (detected locally, waiting to be applied):");
			infos.append("\n=> Pending migrations (detected locally, waiting to be applied):\n");
			logger.info(getLogHeader());
			infos.append(getLogHeader() + "\n");
			for (MigrationInfo migration : info.pending()) {
				logger.info(getLogLine(migration));
				infos.append(getLogLine(migration) + "\n");
			}
		} else {
			logger.info("=> No pending migrations.");
			infos.append("\n=> No pending migrations.\n");
		}

		logger.warn("Info completed!");
		return "OK!<pre>" + infos.toString() + "</pre>";
	}

	private String getLogHeader() {
		String separator = "+------+---------+--------+----------+------------+----------------------+---------------------------------------------------------------";
		return separator + "\n| Rank | Applied | Failed | Resolved |   State    |       Version        | Description\n" + separator;
	}

	private String getLogLine(MigrationInfo migration) {
		return String.format("| %4s | %s | %s | %s | %s | %-20s | %s", migration.getInstalledRank(),
				StringUtils.center(Boolean.toString(migration.getState().isApplied()), 7),
				StringUtils.center(Boolean.toString(migration.getState().isFailed()), 6),
				StringUtils.center(Boolean.toString(migration.getState().isResolved()), 8),
				StringUtils.center(migration.getState().getDisplayName(), 10), StringUtils.center(migration.getVersion().getVersion(), 20),
				migration.getDescription());
	}

}
