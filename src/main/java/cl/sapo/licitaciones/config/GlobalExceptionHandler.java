package cl.sapo.licitaciones.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Global exception handler to prevent stack trace exposure.
 * Implements secure error handling following DevSecOps best practices.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles generic unexpected exceptions.
     * Logs full stack trace but returns generic error to user.
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception occurred at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", "500");
        mav.addObject("errorTitle", "Error Interno del Servidor");
        mav.addObject("errorMessage", "Ha ocurrido un error inesperado. Por favor, intente más tarde.");
        mav.addObject("showDetails", false);
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        
        return mav;
    }

    /**
     * Handles validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ModelAndView handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error at {}: {}", request.getRequestURI(), ex.getMessage());
        
        StringBuilder errorDetails = new StringBuilder("Errores de validación: ");
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errorDetails.append(error.getField())
                       .append(" - ")
                       .append(error.getDefaultMessage())
                       .append("; ")
        );
        
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", "400");
        mav.addObject("errorTitle", "Solicitud Inválida");
        mav.addObject("errorMessage", errorDetails.toString());
        mav.addObject("showDetails", true);
        mav.setStatus(HttpStatus.BAD_REQUEST);
        
        return mav;
    }

    /**
     * Handles invalid argument type errors.
     */
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public ModelAndView handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Bad request at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", "400");
        mav.addObject("errorTitle", "Solicitud Inválida");
        mav.addObject("errorMessage", "Los parámetros enviados no son válidos.");
        mav.addObject("showDetails", false);
        mav.setStatus(HttpStatus.BAD_REQUEST);
        
        return mav;
    }

    /**
     * Handles 404 Not Found errors.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ModelAndView handleNotFound(NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", request.getRequestURI());
        
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", "404");
        mav.addObject("errorTitle", "Página No Encontrada");
        mav.addObject("errorMessage", "La página solicitada no existe.");
        mav.addObject("showDetails", false);
        mav.setStatus(HttpStatus.NOT_FOUND);
        
        return mav;
    }

    /**
     * Handles database/persistence errors.
     */
    @ExceptionHandler({org.springframework.dao.DataAccessException.class})
    public ModelAndView handleDatabaseException(Exception ex, HttpServletRequest request) {
        log.error("Database error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", "503");
        mav.addObject("errorTitle", "Servicio No Disponible");
        mav.addObject("errorMessage", "Error temporal en la base de datos. Por favor, intente más tarde.");
        mav.addObject("showDetails", false);
        mav.setStatus(HttpStatus.SERVICE_UNAVAILABLE);
        
        return mav;
    }
}
