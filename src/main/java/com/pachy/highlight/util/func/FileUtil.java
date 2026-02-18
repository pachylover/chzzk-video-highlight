package com.pachy.highlight.util.func;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;


public class FileUtil {
	public String GetFileIcon(HttpServletRequest request, String fileName){
		String ext = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
		String strFileIcon = "multi_icon_" + ext.toLowerCase() + ".gif";
		
		String DirectoryPath = request.getSession().getServletContext().getRealPath("/"+"resources"+"/"+"icon"+"/");
		String FullPath = DirectoryPath + strFileIcon;
		
		File file = new File(FullPath);
		if(file.exists()) {
			return "/resources/icon/" + strFileIcon;
		}else {
			return "/resources/icon/" + "multi_icon_file.gif";
		}
	}
	
	// 파일 확장자 체크
	public String fileExtCheck(MultipartHttpServletRequest file, String type) {
		Iterator<String> iter = file.getFileNames();
		MultipartFile mFile;
		String result = "Y";

		while (iter.hasNext()) {
			String uploadFileName = iter.next();
			if (uploadFileName == null || uploadFileName.isEmpty()) continue;
			mFile = file.getFile(uploadFileName);
			if (mFile == null || mFile.getOriginalFilename() == null) continue;
			File nfile = new File(mFile.getOriginalFilename());
			
			if(type == null || type.isEmpty()) {
				if(uploadFileName.equals("thumb")) type = "img";
				else if(uploadFileName.equals("ir_vod")) type = "vod";
				else if(uploadFileName.equals("dirlink_upload")) type = "xls";
				else type = "nomal";
			}
			
			if (mFile.getSize() > 0) {
				if (badFileExtIsReturnBoolean(nfile, FileExtType(type))) {
					System.out.println("확장자 통과");
				} else {
					System.out.println("확장자 불통과");
					result = "N";
					break;
				}
			}
		}
		return result;
		
	}
	//확장자 타입
	public String[] FileExtType(String type) {
		String[] temp;
		String[] imgEXT = {"jpg", "jpeg", "gif", "png"};
		String[] vodEXT = {"mp4"};
		String[] xlsEXT = {"xls", "xlsx"};
		String[] pdfEXT = {"pdf"};
		String[] nomalEXT = {"jpg", "jpeg", "gif", "png", "xls", "xlsx", "pdf", "hwp", "ppt", "pptx", "doc", "docx"};
		String[] allEXT = {"jpg", "jpeg", "gif", "png", "hwp", "ppt", "pptx", "xls", "xlsx", "js", "doc", "pdf",
				"docx", "mp4", "wmv", "zip", "arj", "rar", "tar", "7z"};
		
		if(Objects.equals(type, "img")) temp = imgEXT;
		else if(Objects.equals(type, "vod")) temp = vodEXT;
		else if(Objects.equals(type, "xls")) temp = xlsEXT;
		else if(Objects.equals(type, "pdf")) temp = pdfEXT;
		else if(Objects.equals(type, "nomal")) temp = nomalEXT;
		else if (Objects.equals(type, "all")) temp = allEXT;
		else temp = allEXT;
		
		return temp;
	}

	//확장자 체크하는 함수
	public boolean badFileExtIsReturnBoolean(File file, String[] BAD_EXTENSION) {
		String fileName = file.getName();
		String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
		int count = 0;
		
		int len = BAD_EXTENSION.length;
		for (int i = 0; i < len; i++) {
			if (ext.equalsIgnoreCase(BAD_EXTENSION[i])) {
				count++;
				break;
			}
		}

		if (count == 1) return true;
		else return false;
	}

	public static String encodeFileName(String fileName, HttpServletRequest request) {
		String header = request.getHeader("User-Agent");
		if (header.contains("Edge")){
			fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
		} else if (header.contains("MSIE") || header.contains("Trident")) { // IE 11버전부터 Trident로 변경되었기때문에 추가해준다.
			fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
		} else if (header.contains("Chrome")) {
			fileName = new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
		} else if (header.contains("Opera")) {
			fileName = new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
		} else if (header.contains("Firefox")) {
			fileName = new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
		} else if (header.contains("Safari")) {
			fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
		} else {
			fileName = new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
		}
		
		return fileName;
	}
	
	public static String macFileNameProcess(String fileName) {
		return Normalizer.normalize(fileName, Normalizer.Form.NFC);
	}
	
	/**
	 * 파일 유효성 체크 메서드. 파일사이즈가 허용 범위내에 있는지 검사한다.
	 * @param file : 파일
	 * @param size : 허용할 사이즈(MB단위)
	 * @return : 유효성 여부
	 */
	public static boolean fileValidation(MultipartFile file, int size) {
		try {
			if (file == null) return false;
			else return file.getSize() <= (long) size * 1024 * 1024;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * 파일 유효성 체크 메서드. 파일확장자가 받은 확장자 안에 있는지 검사한다.
	 * @param file : 파일
	 * @param extArr : 허용할 확장자(문자열 배열)
	 * @return : 유효성 여부
	 */
	//파일 유효성 체크 메서드(확장자 only)
	public static boolean fileValidation(MultipartFile file, String[] extArr) {
		if (extArr == null || extArr.length == 0) return true; // 검사할 확장자가 없다면 true 리턴
		
		try {
			if (file == null) return false;
			else {
				String fileName; // 파일명
				String ext; // 확장자명
				int dotIndex; // .의 인덱스
				
				fileName = file.getOriginalFilename();
				if (fileName == null) return false;
				
				dotIndex = fileName.lastIndexOf(".");
				ext = fileName.substring(dotIndex + 1);
				
				return Arrays.asList(extArr).contains(ext);
			}
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * 파일 유효성 체크 메서드. 파일사이즈, 파일확장자가 허용 범위내에 있는지 검사한다.
	 * @param file : 파일
	 * @param size : 허용할 사이즈(MB단위)
	 * @param extArr : 허용할 확장자(문자열 배열)
	 * @return : 유효성 여부
	 */
	//파일 유효성 체크 메서드(size, 확장자)
	public static boolean fileValidation(MultipartFile file, int size, String[] extArr) {
		try {
			
			return fileValidation(file, size) && fileValidation(file, extArr);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}