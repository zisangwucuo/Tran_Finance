package com.transfinance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 前端单页应用路由兜底控制器。
 *
 * <p>发布模式下，前端静态资源会被打包进后端 jar。
 * Vue Router 使用 history 模式时，用户刷新子页面会直接请求后端路径，
 * 因此这里把已知页面路由统一转发到 index.html。
 *
 * <p>注意：这里只显式声明前端页面路径，不使用 Spring Boot 3 不兼容的通配写法，
 * 这样既能保留刷新能力，也不会影响 /api 接口。
 */
@Controller
public class FrontendRouteController {

    /**
     * 金融机构端和企业端的所有前端入口路由。
     *
     * <p>这些路由都交给前端 Vue Router 继续处理：
     * <ul>
     *   <li>/login</li>
     *   <li>/register</li>
     *   <li>/company/login</li>
     *   <li>/company/register</li>
     *   <li>/dashboard/*</li>
     *   <li>/company/dashboard/*</li>
     * </ul>
     */
    @GetMapping({
        "/",
        "/login",
        "/financial/login",
        "/register",
        "/financial/register",
        "/company/login",
        "/company/register",
        "/dashboard",
        "/dashboard/electric-company-data",
        "/dashboard/pending-company-data",
        "/dashboard/model-training",
        "/dashboard/federated-learning",
        "/dashboard/data-notices",
        "/company/dashboard",
        "/company/dashboard/history-data",
        "/company/dashboard/pending-data",
        "/company/dashboard/missing-data"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
