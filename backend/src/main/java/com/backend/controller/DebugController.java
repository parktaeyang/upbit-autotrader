package com.backend.controller;

import com.backend.config.UpbitProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {
    private final UpbitProperties props;
    public DebugController(UpbitProperties props) { this.props = props; }

    @GetMapping("/debug/upbit")
    public String check() {
        return props.getAccessKey() + " / " + props.getSecretKey();
    }
}
