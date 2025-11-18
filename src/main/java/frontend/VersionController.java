package frontend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import doda25.team13.VersionUtil;

@RestController
public class VersionController {

    @GetMapping("/version")
    public String getVersion() {
        return VersionUtil.getVersion();
    }
}
