package dev.enrique.bank.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.service.TransferService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class TransferController {
    private final TransferService transferService;
    
}
