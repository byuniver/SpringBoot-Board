package com.bkm.sbb.question;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.bkm.sbb.DataNotFoundException;
import com.bkm.sbb.answer.Answer;
import com.bkm.sbb.category.Category;
import com.bkm.sbb.user.SiteUser;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    /* private Specification<Question> search(String kw, int categoryId) {
        return new Specification<>() {
            private static final long serialVersionUID = 1L;
            @Override
            public Predicate toPredicate(Root<Question> q, CriteriaQuery<?> query, CriteriaBuilder cb) {
                query.distinct(true);  // 중복을 제거 
                Join<Question, SiteUser> u1 = q.join("author", JoinType.LEFT);
                Join<Question, Answer> a = q.join("answerList", JoinType.LEFT);
                Join<Answer, SiteUser> u2 = a.join("author", JoinType.LEFT);
                return cb.or(cb.like(q.get("subject"), "%" + kw + "%"), // 제목 
                        cb.like(q.get("content"), "%" + kw + "%"),      // 내용 
                        cb.like(u1.get("username"), "%" + kw + "%"),    // 질문 작성자 
                        cb.like(a.get("content"), "%" + kw + "%"),      // 답변 내용 
                        cb.like(u2.get("username"), "%" + kw + "%"));   // 답변 작성자 
            }
        };
    } */
    
    private Specification<Question> search(String kw, int categoryId) {
        return new Specification<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Predicate toPredicate(Root<Question> q, CriteriaQuery<?> query, CriteriaBuilder builder) {
                query.distinct(true); //중복제거
                Join<Question, SiteUser> u1 = q.join("author", JoinType.LEFT);
                Join<Question, Answer> a = q.join("answerList", JoinType.LEFT);
                Join<Answer, SiteUser> u2 = a.join("author", JoinType.LEFT);
                return
                        builder.and(builder.equal(q.get("category").get("id"), categoryId),
                                builder.or(builder.like(q.get("subject"), "%" + kw + "%"), //제목
                                        builder.like(q.get("content"), "%" + kw + "%"), //내용
                                        builder.like(u1.get("username"), "%" + kw + "%"), // 질문 작성자
                                        builder.like(a.get("content"), "%" + kw + "%"), // 답변 내용
                                        builder.like(u2.get("username"), "%" + kw + "%"))
                        );
            }

        };
    }
    
    
    public List<Question> getList() {
        return this.questionRepository.findAll();
    }
    
    public Page<Question> getList(int page, String kw) {
    	List<Sort.Order> sorts = new ArrayList<>();
        sorts.add(Sort.Order.desc("createDate"));
        Pageable pageable = PageRequest.of(page, 10, Sort.by(sorts));
        return this.questionRepository.findAllByKeyword(kw, pageable);
    }
    
    public Page<Question> getList(int page, String kw, Category category) {
        List<Sort.Order> sorts = new ArrayList<>();
        sorts.add(Sort.Order.desc("createDate"));
        Pageable pageable = PageRequest.of(page, 10, Sort.by(sorts));
        Specification<Question> spec = search(kw, category.getId());
        //순수 쿼리로 작성한 경우
        //return this.questionRepository.findAllByKeyword(kw, category.getId(), pageable);
        //Specification 사용한 경우
        return this.questionRepository.findAll(spec, pageable);
    }

    /* public Question getQuestion(Integer id) {  
        Optional<Question> question = this.questionRepository.findById(id);
        if (question.isPresent()) {
            return question.get();
        } else {
            throw new DataNotFoundException("question not found");
        }
    } */
    
    public Question getQuestion(Integer id) {
	Optional<Question> question = this.questionRepository.findById(id);
		if (question.isPresent()) 
		{
			Question question1 = question.get();
			question1.setView(question1.getView()+1);	
			this.questionRepository.save(question1);
				return question1;
		} 
		else {
            throw new DataNotFoundException("question not found");
        }
    }

    
    public void create(String subject, String content, SiteUser user, Category category) {
        Question q = new Question();
        q.setSubject(subject);
        q.setContent(content);
        q.setCategory(category);
        q.setCreateDate(LocalDateTime.now());
        q.setAuthor(user);
        this.questionRepository.save(q);
    }
    
    public void modify(Question question, String subject, String content) {
        question.setSubject(subject);
        question.setContent(content);
        question.setModifyDate(LocalDateTime.now());
        this.questionRepository.save(question);
    }
    
    public void delete(Question question) {
        this.questionRepository.delete(question);
    }
    
    public void vote(Question question, SiteUser siteUser) {
        question.getVoter().add(siteUser);
        this.questionRepository.save(question);
    }
    
    public List<Question> getCurrentListByUser(String username, int num) {
        Pageable pageable = PageRequest.of(0, num);
        return questionRepository.findCurrentQuestion(username, pageable);
    }
}