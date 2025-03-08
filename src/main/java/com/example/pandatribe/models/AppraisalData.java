package com.example.pandatribe.models;

import com.example.pandatribe.convertors.JsonToAppraisalResult;
import com.example.pandatribe.models.results.AppraisalResult;
import jakarta.persistence.*;
import lombok.*;

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
}
