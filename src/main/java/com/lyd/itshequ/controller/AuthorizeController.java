package com.lyd.itshequ.controller;

import com.lyd.itshequ.bean.AccessTokenDTO;
import com.lyd.itshequ.bean.GithubUser;
import com.lyd.itshequ.commponent.GithubProvider;
import com.lyd.itshequ.exception.MeErrorCode;
import com.lyd.itshequ.exception.MeExceptions;
import com.lyd.itshequ.mapper.UserMapper;
import com.lyd.itshequ.model.User;
import com.lyd.itshequ.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * @ClassName AuthorizeController
 * @Description TODO
 * @Author Liuyunda
 * @Date 2020/2/21 18:45
 **/
@Controller
@Slf4j
public class AuthorizeController {
	@Autowired
	private GithubProvider githubProvider;
	@Autowired
	private UserService userService;
	@Value("${github.client.id}")
	private String clientId;

	@Value("${github.client.secret}")
	private String clientSecret;

	@Value("${github.redirect.uri}")
	private String redirectUri;

	@Autowired
	private UserMapper userMapper;
	@GetMapping("/callback")
	public String callback(@RequestParam(name = "code")String code,
	                       @RequestParam(name = "state")String state,
	                       HttpServletRequest request, HttpServletResponse response){

		AccessTokenDTO accessTokenDTO = new AccessTokenDTO();
		accessTokenDTO.setRedirect_uri(redirectUri);
		accessTokenDTO.setClient_id(clientId);
		accessTokenDTO.setCode(code);
		accessTokenDTO.setState(state);
		accessTokenDTO.setClient_secret(clientSecret);

		String accessToken = githubProvider.getAccessToken(accessTokenDTO);
		GithubUser githubUser = githubProvider.getUser(accessToken);
		if(githubUser != null&&githubUser.getId()!=null){
			User user = new User();
			String token = UUID.randomUUID().toString();
			user.setToken(token);
			user.setName(githubUser.getName());
			user.setAccountId(String.valueOf(githubUser.getId()));
			user.setAvatarUrl(githubUser.getAvatarUrl());
			userService.createOrUpdate(user);
			User byAccountId = userMapper.findByAccountId(user.getAccountId());
			if (byAccountId.getStatus() == 0){
				throw new MeExceptions(MeErrorCode.USER_BLOCK);
			}
			//登录成功 写入cookie和session
			response.addCookie(new Cookie("token",token));
			// request.getSession().setAttribute("user",githubUser);
			return "redirect:/";
		}else{
			log.error("callback get github error,{}",githubUser);
			//登录失败  重新登录
			return "redirect:/";
		}
	}


	@GetMapping("/logout")
	public String logOut(HttpServletRequest request,HttpServletResponse response){
		request.getSession().removeAttribute("user");

		Cookie cookie = new Cookie("token", null);//删除前必须要new 一个valu为空；
		cookie.setMaxAge(0);//生命周期设置为0
		response.addCookie(cookie);
		return "redirect:/";
	}
}
