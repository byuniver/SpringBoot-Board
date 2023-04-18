package com.bkm.sbb.question;

import java.security.Principal;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.bkm.sbb.answer.Answer;
import com.bkm.sbb.answer.AnswerForm;
import com.bkm.sbb.answer.AnswerService;
import com.bkm.sbb.category.Category;
import com.bkm.sbb.category.CategoryService;
import com.bkm.sbb.user.SiteUser;
import com.bkm.sbb.user.UserService;

import lombok.RequiredArgsConstructor;

@RequestMapping("/question")
@RequiredArgsConstructor		//롬복이 제공하는 애너테이션으로 final이 붙은 속성을 포함하는 생성자를 자동으로 생성하는 역할
@Controller
public class QuestionController {

    private final QuestionService questionService;		// questionService 객체는 생성자 방식으로 DI 규칙에 의해 주입된다.
    private final UserService userService;
    private final AnswerService answerService;
    private final CategoryService categoryService;
    
    @RequestMapping("/list/{category}")
    public String list(Model model,
                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                 @PathVariable("category") String category,
                                 @RequestParam(value = "kw", defaultValue = "") String kw) {

        Category category1 = this.categoryService.getCategoryByTitle(category);
        Page<Question> paging = this.questionService.getList(page, kw, category1);
        //Model 객체는 자바 클래스와 템플릿 간의 연결고리 역할을 한다. Model 객체에 값을 담아두면 템플릿에서 그 값을 사용할 수 있다.
        model.addAttribute("paging", paging);
        model.addAttribute("kw", kw);
        model.addAttribute("category", category);
        return "question_list";
    }
    
    @GetMapping("/list")
    public String list(Model model, @RequestParam(value="page", defaultValue="0") int page, @RequestParam(value = "kw", defaultValue = "") String kw) {
        Page<Question> paging = this.questionService.getList(page, kw);
        model.addAttribute("paging", paging);
        model.addAttribute("kw", kw);
        return "question_list";
    }
    
    /* @GetMapping(value = "/detail/{id}")
    public String detail(Model model, @PathVariable("id") Integer id, AnswerForm answerForm) {
        Question question = this.questionService.getQuestion(id);
        model.addAttribute("question", question);
        return "question_detail";
    } */
    
	@GetMapping(value = "/detail/{id}")
    public String detail(Model model, @PathVariable("id") Integer id, AnswerForm answerForm, 
    		@RequestParam(value = "answerPage", defaultValue = "0") int answerPage) {
		Question question = this.questionService.getQuestion(id);
		Page<Answer> answerPaging =  this.answerService.getList(question, answerPage);
        model.addAttribute("question", question);
        model.addAttribute("answerPaging", answerPaging);
        return "question_detail";
    }
    
    @PreAuthorize("isAuthenticated()") //로그아웃 상태에서는 로그인 페이지로 간다.
    @GetMapping("/create/{category}")
    public String questionCreate(Model model, @PathVariable("category") String category, QuestionForm questionForm) {
        model.addAttribute("category", category);
        return "question_form";
    }
    
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create/{category}")
    public String questionCreate(
            Model model,
            @PathVariable("category") String category,
            @Valid QuestionForm questionForm,
            BindingResult bindingResult,
            Principal principal) {

        //자동으로 QuestionForm의 subject, content에 바인딩된다. (스프링 프레임 워크의 바인딩 기능)
        if (bindingResult.hasErrors()) {
            model.addAttribute("category", category);
            return "question_form";
        }

        SiteUser author = this.userService.getUser(principal.getName());
        Category category1 = this.categoryService.getCategoryByTitle(category);
        this.questionService.create(questionForm.getSubject(), questionForm.getContent(), author, category1);
        return String.format( "redirect:/question/list/%s", category);
    }
        
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/modify/{id}")
    public String questionModify(QuestionForm questionForm, @PathVariable("id") Integer id, Principal principal) {
        Question question = this.questionService.getQuestion(id);
        if(!question.getAuthor().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        questionForm.setSubject(question.getSubject());
        questionForm.setContent(question.getContent());
        return "question_form";
    }
    
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/modify/{id}")
    public String questionModify(@Valid QuestionForm questionForm, BindingResult bindingResult, 
            Principal principal, @PathVariable("id") Integer id) {
        if (bindingResult.hasErrors()) {
            return "question_form";
        }
        Question question = this.questionService.getQuestion(id);
        if (!question.getAuthor().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        this.questionService.modify(question, questionForm.getSubject(), questionForm.getContent());
        return String.format("redirect:/question/detail/%s", id);
    }
    
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/delete/{id}")
    public String questionDelete(Principal principal, @PathVariable("id") Integer id) {
        Question question = this.questionService.getQuestion(id);
        if (!question.getAuthor().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제권한이 없습니다.");
        }
        this.questionService.delete(question);
        return "redirect:/";
    }
    
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/vote/{id}")
    public String questionVote(Principal principal, @PathVariable("id") Integer id) {
        Question question = this.questionService.getQuestion(id);
        SiteUser siteUser = this.userService.getUser(principal.getName());
        this.questionService.vote(question, siteUser);
        return String.format("redirect:/question/detail/%s", id);
    }
}