package com.vmware.blackpearl.proxy;

import static org.junit.Assert.*;

import org.apache.camel.CamelContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.vmware.blackpearl.proxy.CamelServletJmsSampleApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CamelServletJmsSampleApplication.class)
@WebAppConfiguration
public class CamelServletJmsSampleApplicationTests {
    
    @Autowired
    private CamelContext camelContext;

	@Test
	public void contextLoads() {
        assertTrue(true);
	}

}
