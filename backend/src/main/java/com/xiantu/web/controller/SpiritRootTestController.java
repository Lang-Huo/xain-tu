package com.xiantu.web.controller;

import com.xiantu.common.Result;
import com.xiantu.service.SpiritRootTestService;
import com.xiantu.service.SpiritRootTestService.CommitResult;
import com.xiantu.web.dto.SpiritRootQuestionView;
import com.xiantu.web.dto.SpiritRootTestRequest;
import com.xiantu.web.dto.SpiritRootTestResultResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/spirit-root")
public class SpiritRootTestController {

    private final SpiritRootTestService service;

    public SpiritRootTestController(SpiritRootTestService service) {
        this.service = service;
    }

    /** 获取灵根测试题目（5 题，不含权重）。 */
    @GetMapping("/questions")
    public Result<List<SpiritRootQuestionView>> questions() {
        return Result.ok(service.getQuestions());
    }

    /** 提交测试结果：算分 + 判定 + 落库。 */
    @PostMapping("/test")
    public Result<SpiritRootTestResultResponse> test(@Valid @RequestBody SpiritRootTestRequest req,
                                                     Authentication auth) {
        CommitResult r = service.commit(auth.getName(), req.getAnswers());
        SpiritRootTestResultResponse out = new SpiritRootTestResultResponse();
        out.setRoots(r.roots);
        out.setDual(r.dual);
        return Result.ok(out);
    }
}