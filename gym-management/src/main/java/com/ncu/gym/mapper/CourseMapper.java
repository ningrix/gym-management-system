package com.ncu.gym.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncu.gym.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
