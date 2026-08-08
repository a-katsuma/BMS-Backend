package com.example.app.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

	private Integer docId; // doc_id
	private Integer projectId; // project_id
	private Integer clientId; // client_id

	private String docFilePath; // doc_file_path
	private String docTitle; // doc_title
	private String docType; // doc_type
	private String docRemarks; // doc_remarks
	private LocalDateTime docCreatedAt; // doc_created_at

}
