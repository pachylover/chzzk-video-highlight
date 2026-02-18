package com.pachy.highlight.util.func;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;

import java.util.List;

public class WebUtil {
	
	/**
	 * 클라이언트 IP 주소를 가져오는 메서드
	 * @param request 클라이언트 요청 객체
	 * @return 클라이언트 IP 주소
	 */
	public static String getRemoteIP(HttpServletRequest request){
		String ip = request.getHeader("X-FORWARDED-FOR");
		
		//proxy 환경일 경우
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		
		//웹로직 서버일 경우
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr() ;
		}
		
		if (ip != null) {
			String[] ipArr = ip.split(",");
			if (ipArr.length > 0) ip = ipArr[ipArr.length - 1]; //X-FORWARDED-FOR 에서 두번째 IP를 가져올 경우
			
			return ip.trim();
		} else return null;
	}

	public static String getRemoteIP(ServletRequest request){
		if (request instanceof HttpServletRequest) {
			return getRemoteIP((HttpServletRequest) request);
		} else {
			return null;
		}
	}
	
	/**
	 * 클라이언트 IP 주소를 가져오는 메서드
	 * @param request 클라이언트 요청 객체
	 * @return 클라이언트 IP 주소
	 */
	public static String getRemoteIP(ServerHttpRequest request){
		List<String> ipList = request.getHeaders().get("X-FORWARDED-FOR");
		String ip = ipList != null ? ipList.getFirst() : null;
		
		if (request.getURI().getScheme().equals("http")) ip = request.getRemoteAddress().getHostString();
		
		//proxy 환경일 경우
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ipList = request.getHeaders().get("Proxy-Client-IP");
			ip = ipList != null ? ipList.getFirst() : null;
		}
		
		//웹로직 서버일 경우
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ipList = request.getHeaders().get("WL-Proxy-Client-IP");
			ip = ipList != null ? ipList.getFirst() : null;
		}
		
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ipList = request.getHeaders().get("HTTP_X_FORWARDED_FOR");
			ip = ipList != null ? ipList.getFirst() : null;
		}
		
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = String.valueOf(request.getRemoteAddress().getHostString());
		}
		
		if (ip != null) {
			String[] ipArr = ip.split(",");
			if (ipArr.length > 0) ip = ipArr[ipArr.length - 1]; //X-FORWARDED-FOR 에서 두번째 IP를 가져올 경우
			
			return ip.trim();
		} else return null;
	}
	/**
	 * 허용 IP인지 여부를 확인하는 메서드
	 * @param accessibleIP 허용 IP, 단일 아이피인 경우 IP만, 대역인 경우 IP/대역
	 * @param clientIP 접속한 클라이언트 IP
	 * @return 허용 IP인지 여부
	 */
	public static boolean isAccessibleIP(String accessibleIP, String clientIP) {
		if (accessibleIP == null || accessibleIP.isEmpty()) return true;
		
		//허용 ip를 /로 구분해서 배열로 만듬
		String[] ipInfo = accessibleIP.split("/");
		//허용 ip가 단일 IP일 경우
		if (ipInfo.length == 1) return clientIP.equals(ipInfo[0]);
		
		//허용 ip가 대역일 경우
		switch (ipInfo[1]) {
			case "24":
				//마지막 .을 찾아서 그 전까지 비교
				return clientIP.startsWith(ipInfo[0].substring(0, ipInfo[0].lastIndexOf(".")));
			case "16":
				//두번째 .을 찾아서 그 전까지 비교
				return clientIP.startsWith(ipInfo[0].substring(0, ipInfo[0].indexOf(".", ipInfo[0].indexOf(".") + 1)));
			case "8":
				return clientIP.startsWith(ipInfo[0].substring(0, ipInfo[0].indexOf(".")));
			case "0":
				return true;
			default:
				return false;
		}
	}
}