package com.example.pandatribe.scheduledjobs;

import com.example.pandatribe.models.dbmodels.appraisal.AppraisalData;
import com.example.pandatribe.models.dbmodels.industry.BlueprintData;
import com.example.pandatribe.repositories.interfaces.AppraisalDataRepository;
import com.example.pandatribe.repositories.interfaces.BlueprintDataRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Service
@Slf4j
public class ScheduledJobsService {
    public static final int OLDER_THAN_DAYS = 7;
    public static final String RUN_EVERY_SUNDAY_AT_4_AM ="0 0 4 * * SUN";

    private final AppraisalDataRepository appraisalDataRepository;
    private final BlueprintDataRepository blueprintDataRepository;

    @Scheduled(cron = RUN_EVERY_SUNDAY_AT_4_AM, zone = "UTC")
    public void runWeeklyTask(){
        scheduledRemoveAppraisals();
        scheduledRemoveBlueprintData();
    }

    private void scheduledRemoveBlueprintData(){
        LocalDate currentDate = LocalDate.now();
        LocalDate tempDate = currentDate.minusDays(OLDER_THAN_DAYS);
        Date olderThanSevenDays = Date.from(tempDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        List<BlueprintData> blueprintsToRemove = blueprintDataRepository.findByCreationDateBefore(olderThanSevenDays);
        blueprintDataRepository.deleteAll(blueprintsToRemove);
        log.info("Removed " + blueprintsToRemove.size() + " blueprint data");
    }

    private void scheduledRemoveAppraisals(){
        LocalDate currentDate = LocalDate.now();
        LocalDate tempDate = currentDate.minusDays(OLDER_THAN_DAYS);
        Date olderThanSevenDays = Date.from(tempDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        List<AppraisalData> appraisalsToRemove = appraisalDataRepository.findAppraisalDataByCreationDateBefore(olderThanSevenDays);
        appraisalDataRepository.deleteAll(appraisalsToRemove);
        log.info("Removed " + appraisalsToRemove.size() + " appraisal data");
    }
}
