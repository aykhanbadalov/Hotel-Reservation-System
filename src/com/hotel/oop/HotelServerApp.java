package com.hotel.oop;

import com.hotel.oop.http.ApiHttpHandler;
import com.hotel.oop.http.HotelApiFacade;
import com.hotel.oop.http.StaticFileHandler;
import com.hotel.oop.models.interfaces.Payable;
import com.hotel.oop.persistence.HotelDataRoot;
import com.hotel.oop.persistence.HotelFilePersistence;
import com.hotel.oop.patterns.notification.EmailNotificationObserver;
import com.hotel.oop.patterns.notification.ReservationSubject;
import com.hotel.oop.patterns.notification.SmsNotificationObserver;
import com.hotel.oop.patterns.pricing.PricingStrategyFactory;
import com.hotel.oop.services.AuthService;
import com.hotel.oop.services.PaymentService;
import com.hotel.oop.services.ReservationService;
import com.hotel.oop.services.RoomCatalogService;
import com.hotel.oop.services.SessionManager;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * Application entry point — loads persisted state from {@code data/hotel-state.dat} (or seeds demo rooms once).
 */
public class HotelServerApp {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        int port = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "8080")
        );
        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }

        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path publicDir = projectRoot.resolve("frontend/public").normalize();
        Path dataDir = projectRoot.resolve("data");

        HotelDataRoot dataRoot = new HotelDataRoot();
        HotelFilePersistence.loadOrSeed(dataDir, dataRoot);

        Runnable persist = () -> {
            try {
                HotelFilePersistence.save(dataDir, dataRoot);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        SessionManager sessions = new SessionManager();
        RoomCatalogService catalog = new RoomCatalogService(dataRoot, persist);
        AuthService auth = new AuthService(dataRoot, sessions, persist);

        PricingStrategyFactory pricingFactory = new PricingStrategyFactory();
        ReservationSubject reservationSubject = new ReservationSubject();
        new EmailNotificationObserver(reservationSubject);
        new SmsNotificationObserver(reservationSubject);

        Payable payment = new PaymentService();
        ReservationService reservationService = new ReservationService(
                dataRoot, catalog, payment, pricingFactory, reservationSubject, auth, persist);

        HotelApiFacade api = new HotelApiFacade(catalog, reservationService, auth, sessions);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        // Use trailing slash so "/api-client.js" is not misrouted to API handler.
        server.createContext("/api/", new ApiHttpHandler(api));
        server.createContext("/", new StaticFileHandler(publicDir));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("Hotel OOP server listening on http://localhost:" + port + "/");
        System.out.println("Data directory: " + dataDir.toAbsolutePath());
        System.out.println("Static root: " + publicDir);
    }
}
