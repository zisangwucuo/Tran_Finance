package com.transfinance.controller;

import com.transfinance.common.ApiResponse;
import com.transfinance.dto.MaterialAdoptRequest;
import com.transfinance.exception.BusinessException;
import com.transfinance.service.MaterialExtractionService;
import com.transfinance.vo.MaterialExtractionVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

/** 材料读取、提取和采用统一从服务端会话识别企业，不接受浏览器传入企业编码。 */
@RestController
@RequestMapping("/api/company-materials")
public class MaterialExtractionController {
    private final MaterialExtractionService service;
    public MaterialExtractionController(MaterialExtractionService service) { this.service=service; }
    private String owner(HttpServletRequest request) {
        var session=request.getSession(false);
        Object code=session==null?null:session.getAttribute("companyCode");
        if(!(code instanceof String s) || s.isBlank()) throw new BusinessException(401,"请退出并重新登录企业账号以使用材料提取");
        return s;
    }
    @GetMapping("/template") public ApiResponse<Map<String,Object>> template(HttpServletRequest r) { return ApiResponse.success(service.template(owner(r))); }
    @GetMapping public ApiResponse<List<MaterialExtractionVO>> list(HttpServletRequest r) { return ApiResponse.success(service.list(owner(r))); }
    @GetMapping("/{id}") public ApiResponse<MaterialExtractionVO> get(@PathVariable String id,HttpServletRequest r) { return ApiResponse.success(service.get(owner(r),id)); }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String,String>> upload(@RequestParam MultipartFile file,@RequestParam int year,
            @RequestParam boolean consent,HttpServletRequest r) {
        return ApiResponse.success(Map.of("id",service.upload(owner(r),year,consent,file)));
    }
    @GetMapping("/{id}/file") public ResponseEntity<FileSystemResource> file(@PathVariable String id,HttpServletRequest r) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
                .header("Content-Disposition","inline; filename=material.pdf")
                .body(new FileSystemResource(service.file(owner(r),id)));
    }
    @GetMapping("/{id}/targets") public ApiResponse<Map<String,Object>> targets(@PathVariable String id,HttpServletRequest r) {
        return ApiResponse.success(service.targets(owner(r),id));
    }
    @PostMapping("/{id}/adoptions") public ApiResponse<Void> adopt(@PathVariable String id,
            @Valid @RequestBody MaterialAdoptRequest body,HttpServletRequest r) {
        service.adopt(owner(r),id,body); return ApiResponse.success();
    }
}
