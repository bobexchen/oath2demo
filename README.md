---
title: Oauth2学习笔记
date: 17:05 2019/4/7
tags: oauth2
---

### oauth2根据使用场景不同，分成了4种模式

授权码模式（authorization code） 商用模式主要使用授权码模式  
简化模式（implicit）  
密码模式（resource owner password credentials）  
客户端模式（client credentials）  

### 使用oauth2保护你的应用，可以分为简易的分为三个步骤

配置资源服务器  
配置认证服务器  
配置spring security  

### 写一个简单的密码模式的demo如下：

首先给出我的依赖：

	    <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- 注意是starter,自动配置 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <!-- 不是starter,手动配置 -->
        <dependency>
            <groupId>org.springframework.security.oauth</groupId>
            <artifactId>spring-security-oauth2</artifactId>
            <version>2.3.2.RELEASE</version>
        </dependency>
        <!-- 将token存储在redis中 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

配置资源服务器：

    @Configuration
    @EnableResourceServer
    public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {
    private static final String DEMO_RESOURCE_ID = "order";

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        resources.resourceId(DEMO_RESOURCE_ID).stateless(true);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http
                // Since we want the protected resources to be accessible in the UI as well we need
                // session creation to be allowed (it's disabled by default in 2.0.6)
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .and()
                .requestMatchers().anyRequest()
                .and()
                .anonymous()
                .and()
                .authorizeRequests()
    //              .antMatchers("/product/**").access("#oauth2.hasScope('select') and hasRole('ROLE_USER')")
                .antMatchers("/order/**").authenticated();//配置order访问控制，必须认证过后才可以访问
    //             @formatter:on
        }
    }

配置认证服务器：  

    @Configuration
    @EnableAuthorizationServer
    public class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {
    private static final String DEMO_RESOURCE_ID = "order";

    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        //配置两个客户端,一个用于password认证一个用于client认证
        clients.inMemory().withClient("client_1")
                .resourceIds(DEMO_RESOURCE_ID)
                .authorizedGrantTypes("client_credentials", "refresh_token")
                .scopes("select")
                .authorities("client")
                .secret("{noop}123456")
                .and().withClient("client_2")
                .resourceIds(DEMO_RESOURCE_ID)
                .authorizedGrantTypes("password", "refresh_token")
                .scopes("select")
                .authorities("client")
                .secret("{noop}123456");
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
                .tokenStore(new RedisTokenStore(redisConnectionFactory))
                .authenticationManager(authenticationManager);
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
        //允许表单认证
        oauthServer.allowFormAuthenticationForClients();
    }
}

配置spring security

    @Configuration
    @EnableWebSecurity
    public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
    @Bean
    @Override
    protected UserDetailsService userDetailsService(){
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        manager.createUser(User.withUsername("user_1").password("{noop}123456").authorities("USER").build());
        manager.createUser(User.withUsername("user_2").password("{noop}123456").authorities("USER").build());
        return manager;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http
                .requestMatchers().anyRequest()
                .and()
                .authorizeRequests()
                .antMatchers("/oauth/*").permitAll();
        // @formatter:on
    }

    @Bean
    @Override
        public AuthenticationManager authenticationManagerBean() throws Exception {
            AuthenticationManager manager = super.authenticationManagerBean();
            return manager;
        }
    }
    
Postman测试接口

1.使用http://localhost:8080/demo/1会返回参数  
2.使用http://localhost:8080/resource/1则会显示未经过认证  
3.密码模式通过  
    http://localhost:8080/oauth/token?username=user_1&password=123456&grant_type=password&scope=select&client_id=client_2&client_secret=123456
    获得token  
        {
            "access_token": "e9492791-beff-4f96-9ff7-8b6e52bfafb2",
            "token_type": "bearer",
            "refresh_token": "f0891fcb-77e6-4c3a-a37d-d75f0a6432f0",
            "expires_in": 43199,
            "scope": "select"
        }
        其中expires_in为有效时长，单位为秒，access_token为令牌密钥
   客户端模式通过
   http://localhost:8080/oauth/token?grant_type=client_credentials&scope=select&client_id=client_1&client_secret=123456
   获取token
    {
        "access_token": "091c4fea-56bb-433e-b94f-2c0dd19bc286",
        "token_type": "bearer",
        "expires_in": 43199,
        "scope": "select"
     }
4.resource添加token则能返回对应的结果，例：   
    http://localhost:8080/resource/1?access_token=e9492791-beff-4f96-9ff7-8b6e52bfafb2
    
    
client模式，没有用户的概念，直接与认证服务器交互，用配置中的客户端信息去申请accessToken，客户端有自己的client_id,client_secret对应于用户的username,password，而客户端也拥有自己的authorities，当采取client模式认证时，对应的权限也就是客户端自己的authorities。
password模式，自己本身有一套用户体系，在认证时需要带上自己的用户名和密码，以及客户端的client_id,client_secret。此时，accessToken所包含的权限是用户本身的权限，而不是客户端的权限。
    
1、Client模式并不存在对个体用户授权的行为，被授权的主体为client。因此，该模式可用于对某类用户进行集体授权。

    
 参考博客：[从零开始的Spring Security Oauth2](http://blog.didispace.com/spring-security-oauth2-xjf-1/)   
           [spring security实战 3](https://segmentfault.com/a/1190000015338925)