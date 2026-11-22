package com.example.pandatribe.models.dbmodels.appraisal;

import com.example.pandatribe.convertors.JsonToAppraisalResult;
import com.example.pandatribe.models.results.AppraisalResult;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.Date;
import java.util.UUID;

@Getter
@Entity
@RequiredArgsConstructor
@Builder
@With
@Table(name = "appraisals")
@AllArgsConstructor
public class AppraisalData {

    @Id
    @Column(name="id")
    private UUID id;

    @Column(name = "appraisal_result", columnDefinition = "longtext")
    @Convert(converter = JsonToAppraisalResult.class)
    private AppraisalResult appraisalResult;

    @Column(name="creation_date")
    private Date creationDate;

    @Column(name="transaction_type")
    private String transactionType;

    @Column(name="comment")
    private String comment;

    @Column(name="market")
    private String market;

    @Column(name="price_percentage")
    private Double pricePercentage;

    @Column(name="system")
    private String system;
}
