package com.example.app.controller.contractee;

import org.springframework.web.bind.annotation.ControllerAdvice;

import com.example.app.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalContracteeControllerAdvice {
	
	private final UserMapper userMapper;
	
//全てのコントローラーが動く前に、このメソッドが実行されてモデルに currentUser が入る
//  @ModelAttribute
//  public void addGlobalAttributes(Model model) {
//      User currentUser = userMapper.selectById(2); // 疑似ログインID
//      model.addAttribute("currentUser", currentUser);
//  }

}
