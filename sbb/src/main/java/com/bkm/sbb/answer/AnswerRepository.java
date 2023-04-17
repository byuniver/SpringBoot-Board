package com.bkm.sbb.answer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bkm.sbb.question.Question;

public interface AnswerRepository extends JpaRepository<Answer, Integer> {
	Page<Answer> findAllByQuestion(Question question, Pageable pagealbe);
}
