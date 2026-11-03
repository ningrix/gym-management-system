package com.ncu.gym.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncu.gym.entity.Course;
import com.ncu.gym.mapper.CourseMapper;
import org.springframework.stereotype.Service;

@Service
public class CourseService extends ServiceImpl<CourseMapper, Course> {
}
