package com.thehecklers.airportservice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class AirportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AirportServiceApplication.class, args);
    }

    // Populate database with all Class B, C, D within n nm of KSTL?
    @Bean
    CommandLineRunner loadData(AirportService service, AirportRepository repo) {
        return args -> {
            Flux.just("KSTL", "KSUS", "KCPS", "KALN", "KBLV", "KCOU", "KJEF", "KSPI", "KDEC", "KCMI", "KMDH", "KMWA", "KCGI", "KTBN")
                    .map(service::retrieveAirport)
                    .flatMap(repo::save)
                    .subscribe();
        };
    }
}

@RestController
@RequiredArgsConstructor
class AirportController {
    @NonNull
    private final AirportService service;

    // Sample values served up by Config Service
    @Value("${testplane:Archer}")
    private String defaultAircraft;
    @Value("${test.plane:Arrow}")
    private String defaultComplexAircraft;
    @Value("${airport:KABC}")
    private String defaultAirport;
    @Value("${fbo.fuel:avgas}")
    private String defaultFuel;

    @GetMapping("/testplane")
    public String tp() {
        return defaultAircraft;
    }

    @GetMapping("/testcomplexplane")
    public String tcp() {
        return defaultComplexAircraft;
    }

    @GetMapping("/testairport")
    public String ap() {
        return defaultAirport;
    }

    @GetMapping("/testfuel")
    public String fuel() {
        return defaultFuel;
    }

    @GetMapping
    Flux<Airport> getAllAirports() {
        System.out.println(">>>> getAllAirports()");
        return service.getAllAirports()
                .log();
    }

    @GetMapping("/list")
    Flux<String> getAirportSummary() {
        System.out.println(">>>> getAirportSummary() - /list");

        return service.getAllAirports()
                .map(ap -> ap.getIcao() + ", " + ap.getName() + "\n")
                .log();
    }

    @GetMapping("/airport/{id}")
    Mono<Airport> getAirportById(@PathVariable String id) {
        System.out.println(">>>> getAirportById() - /airport/{id}");

        return service.getAirportById(id)
                .log();
    }
}

@Service
@RequiredArgsConstructor
class AirportService {
    @Value("${avwx-token:NoValidTokenRetrieved}")
    private String token;
    private final WebClient client = WebClient.create("https://avwx.rest/api/station/");

    @NonNull
    private final AirportRepository repo;

    public final Flux<Airport> getAllAirports() {
        return repo.findAll();
    }

    public final Mono<Airport> getAirportById(String id) {
        return repo.findById(id);
    }

    public final Mono<Airport> retrieveAirport(String id) {
        return client.get()
                .uri(id + "?token=" + token)
                .retrieve()
                .bodyToMono(Airport.class);
    }
}

@Repository
class AirportRepository {
    private Flux<Airport> apFlux = Flux.empty();

    public final Flux<Airport> findAll() {
        return apFlux;
    }

    public final Mono<Airport> findById(String id) {
        return apFlux.filter(ap -> ap.getIcao().equalsIgnoreCase(id))
                .next();
    }

    public final Mono<Airport> save(Mono<Airport> airportMono) {
        apFlux = apFlux.concatWith(airportMono);

        return airportMono;
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
class Airport {
    private String icao;
    private String city, state, elevation_ft, name;
    private double latitude, longitude;
    private Iterable<Runway> runways;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
class Runway {
    private String ident1, ident2;
    private int length_ft, width_ft;
}