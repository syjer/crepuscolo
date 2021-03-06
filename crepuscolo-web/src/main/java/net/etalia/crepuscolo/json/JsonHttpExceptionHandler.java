package net.etalia.crepuscolo.json;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import net.etalia.crepuscolo.auth.AuthData;
import net.etalia.crepuscolo.check.AuthException;
import net.etalia.crepuscolo.utils.ChainMap;
import net.etalia.crepuscolo.utils.HttpException;
import net.etalia.jalia.spring.JaliaJsonView;

public class JsonHttpExceptionHandler implements HandlerExceptionResolver, Ordered {

	protected Log log = LogFactory.getLog(JsonHttpExceptionHandler.class);

	private int order = -1;

	@Override
	@SuppressWarnings("deprecation")
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		
		request.setAttribute("retryException", ex);
		
		ResponseStatus responseStatus = AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class);
		HttpException htex = null;
		if (ex instanceof AuthException) {
			htex = new HttpException().statusCode(((AuthException) ex).getStatusCode());
		} else if (!(ex instanceof HttpException)) {
			htex = new HttpException().cause(ex);
			log.error("Found an Error", ex);
		} else {
			htex = (HttpException) ex;
		}

		try {
			int statusCode = htex.getStatusCode();
			String reason = htex.getMessage();
			String errorCode = htex.getErrorCode();
			
			if (!htex.hasSetStatusCode() && responseStatus != null) {
				statusCode = responseStatus.value().value();
			}
			
			if (!StringUtils.hasLength(reason) && responseStatus != null) {
				reason = responseStatus.reason();
			}
			if (!StringUtils.hasLength(reason)) {
				reason = ex.getMessage();
			}

			if (statusCode == 401) {
				response.setHeader("WWW-Authenticate", AuthData.AUTHENTICATION_SIGN + " realm=\"application\"");
			}
			if (!StringUtils.hasLength(reason)) {
				response.setStatus(statusCode);
			}
			else {
				response.setStatus(statusCode, errorCode);
			}
			
			if (!StringUtils.hasLength(errorCode)) errorCode = "ERROR";
			ChainMap<Object> errmap = new ChainMap<Object>("code", errorCode).add("message", reason);
			
			if (log.isDebugEnabled()) {
				StringWriter sw = new StringWriter();
				htex.printStackTrace(new PrintWriter(sw));
				sw.close();
				errmap.add("stack", sw.toString());
			}
			
			if (htex.getProperties() != null) {
				errmap.putAll(htex.getProperties());
			}
			
			return new ModelAndView(new JaliaJsonView(), errmap);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}
