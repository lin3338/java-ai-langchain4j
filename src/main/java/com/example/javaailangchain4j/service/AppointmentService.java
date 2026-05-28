package com.example.javaailangchain4j.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.example.javaailangchain4j.entity.Appointment;
import org.springframework.stereotype.Service;

@Service
public interface AppointmentService extends IService<Appointment> {
    Appointment getOne(Appointment appointment);

}
