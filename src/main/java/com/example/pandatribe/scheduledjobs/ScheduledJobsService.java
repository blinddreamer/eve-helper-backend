package com.example.pandatribe.scheduledjobs;

import com.example.pandatribe.models.AppraisalData;
import com.example.pandatribe.models.BlueprintData;
import com.example.pandatribe.repositories.interfaces.AppraisalDataRepository;
import com.example.pandatribe.repositories.interfaces.BlueprintDataRepository;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Service
public class ScheduledJobsService {
    private final AppraisalDataRepository appraisalDataRepository;
    private final BlueprintDataRepository blueprintDataRepository;

    @Scheduled(cron = "0 0 4 * * SUN", zone = "UTC")
    public void runWeeklyTask(){
        scheduledRemoveAppraisals();
        scheduledRemoveBlueprintData();
    }

    private void scheduledRemoveBlueprintData(){
        LocalDate currentDate = LocalDate.now();
        LocalDate tempDate = currentDate.minusDays(7);
        Date olderThanSevenDays = Date.from(tempDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        List<BlueprintData> blueprintsToRemove = blueprintDataRepository.findByCreationDateBefore(olderThanSevenDays);
        blueprintDataRepository.deleteAll(blueprintsToRemove);
    }

    private void scheduledRemoveAppraisals(){
        LocalDate currentDate = LocalDate.now();
        LocalDate tempDate = currentDate.minusDays(7);
        Date olderThanSevenDays = Date.from(tempDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        List<AppraisalData> appraisalsToRemove = appraisalDataRepository.findAppraisalDataByCreationDateBefore(olderThanSevenDays);
        appraisalDataRepository.deleteAll(appraisalsToRemove);
    }
}
