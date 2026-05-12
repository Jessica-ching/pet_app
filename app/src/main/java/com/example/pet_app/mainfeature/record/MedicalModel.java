package com.example.pet_app.mainfeature.record;

public class MedicalModel {
    private int recordId;
    private String date;
    private String clinicName;
    private String reason;

    public MedicalModel(int recordId, String date, String clinicName, String reason) {
        this.recordId = recordId;
        this.date = date;
        this.clinicName = clinicName;
        this.reason = reason;
    }

    public int getRecordId() { return recordId; }
    public String getDate() { return date != null ? date : "未知日期"; }
    public String getClinicName() { return clinicName != null ? clinicName : "未填寫診所"; }
    public String getReason() { return reason != null ? reason : "未填寫原因"; }
}