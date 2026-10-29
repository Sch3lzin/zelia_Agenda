package com.zelia.agenda.controllers;

import com.zelia.agenda.domain.user.CreateUserDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @PostMapping("/criar")
    public void criar(@RequestBody @Valid CreateUserDTO dto) {

    }

}
