package Domain;

import Domain.AuctionMechanism.Auction;
import Domain.AuctionMechanism.Bid;
import Domain.AuctionMechanism.LifeCycleAuctionTask;
import Domain.AuctionMechanism.Lot;
import Domain.People.User;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SystemAuctionHouse extends UnicastRemoteObject implements Proxy {

    private ConcurrentHashMap<String,User> usersList;
    private ConcurrentHashMap<Integer,Auction> auctionList;
    private HashMap<Integer,Auction> closedAuction;
    private HashMap<LifeCycleAuctionTask, Long> timerTasks;
    private int auctionIdCounter = 0; //Fare attenzione, ogni volta che spengo il server il valore non e' salvato
    private transient Timer timer;
    private static final String USERS_FILE = "utenti.bin";
    private static final String AUCTION_FILE = "auctions.bin";
    private static final String TIMERS_FILE = "timers.bin";
    private static final String CLOSED_AUCTION = "closedAuction.bin";


    public void createUser(String username, String password){
            User user = new User(username,password);
            addUser(user);
    }

    public boolean logoutS(String username) {
        userListed(username).setLoggedIn(false);
        return true;
    }


    public void addUser(User user){
        usersList.put(user.getUsername(),user);
    }


    /**
     * Il metodo effettua il controllo sull'utilizzo dell'username.
     * Se l'username e' gia' utilizzato chiede all'utente di inserirne uno nuovo finche non ne trova uno valido.
     * @param username
     *
     */
    public boolean alredyTakenUsername(String username){
        if(usersList.containsKey(username))
            return true;
        else
            return false;
    }

    public String showAllActiveAuctions() {
        String toPrint = "";
        for (Map.Entry<Integer,Auction> entry : auctionList.entrySet()) {
            Auction entryValue = entry.getValue();
            toPrint =  toPrint + entryValue.auctionInformation();
        }
        if(auctionList.isEmpty()) {
            toPrint = "Nessun Inserzione Esistente" + "\n";
        }
        return toPrint;
    }

    public String showClosedAuctions() {
        String toPrint = "";
        for (Map.Entry<Integer,Auction> entry : closedAuction.entrySet()) {
            Auction entryValue = entry.getValue();
            toPrint =  toPrint + entryValue.closedAuctionInformation();
        }
        if(closedAuction.isEmpty()) {
            toPrint = "Nessun Inserzione Chiusa" + "\n";
        }
        return toPrint;
    }

    public void addAuction(String title, int price, String vendor, LocalDateTime closingTime) {
        Lot lot = new Lot(title,price,vendor);
        Auction au = new Auction(auctionIdCounter,lot,closingTime);
        auctionList.put(auctionIdCounter,au);
        // Timer for ending the auction
        ZonedDateTime zdt = closingTime.atZone(ZoneId.of("Europe/Rome"));
        long millis = zdt.toInstant().toEpochMilli();
        LifeCycleAuctionTask t = new LifeCycleAuctionTask(auctionIdCounter,millis);
        t.passArgument(auctionList,closedAuction,timerTasks);
        timer.schedule(t, (millis - System.currentTimeMillis()));
        timerTasks.put(t, millis );

        auctionIdCounter++;
    }

    /**
     * Il metodo controlla se e' gia' loggato un utente nel servizio, in tal caso consiglia il logout
     * Il metodo effettua il controllo sulla presenza effettiva nella lista utente, altrimenti non permette il login
     * Se le due condizioni sopra non si avverano allora permette il login
     * @param username
     * @return
     */
    public boolean checkLogin(String username,String pass) {
        User userToCheck = userListed(username);
        if(userToCheck != null) {
            if (userToCheck.checkPassword(pass)) {
                userToCheck.setLoggedIn(true);
                return true;
            }
            return false;
        }
        return false;
    }

    private User userListed(String username) {
        if(usersList.containsKey(username))
            return usersList.get(username);
        else
            return null;
    }

    public boolean checkExistingAuction (int idAuction) {
        if (auctionList.containsKey(idAuction))
            return true;

        else
            return false;
    }

    public int higherOffer(int id) {
        if (auctionList.containsKey(id))
            return auctionList.get(id).getHigherOffer();
        else
            return -1;
    }

    public boolean vendorOfAuction(int idAuction,String logged) {
        if(auctionListed(idAuction).getLot().getVendor().equalsIgnoreCase(logged)) {
            return true;
        }
        return false;
    }


    private Auction auctionListed(int idAuction) {
        if(auctionList.containsKey(idAuction))
            return auctionList.get(idAuction);
        else
            return null;
    }


    public void makeBid(String user, int amount,int id){
        Bid bid = new Bid(user,amount);
        Auction request = auctionListed(id);
        request.addBid(bid);
    }

    public void probe() throws RemoteException {}

    public ConcurrentHashMap<Integer, Auction> getAuctionList() { return auctionList; }

    public HashMap<Integer, Auction> getClosedAuction() { return closedAuction; }

    public HashMap<LifeCycleAuctionTask, Long> getTimerTasks() { return timerTasks; }

    public String saveState()  {
        String result = "";
        try {
            ObjectOutputStream o = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(AUCTION_FILE)));
            o.writeObject(auctionList);
            o.close();
            ObjectOutputStream o2 = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(USERS_FILE)));
            o2.writeObject(usersList);
            o2.close();
            ObjectOutputStream o3 = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(TIMERS_FILE)));
            o3.writeObject(timerTasks);
            o3.close();
            ObjectOutputStream o4 = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(CLOSED_AUCTION)));
            o4.writeObject(closedAuction);
            o4.close();

            result = "Auction state saved in: " + AUCTION_FILE + "\nUsers state saved in: " + USERS_FILE;
        }catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String loadState() throws RemoteException,ClassNotFoundException {
        String result = "";
        loadAuction();
        loadUser();
        loadTimer();
        loadClosedAuction();

        reloadTimer();

        result = "Auction state restored from: " + AUCTION_FILE + "\nUsers state restored from: " + USERS_FILE;

        return result;
    }

    private void loadAuction() {
        try {
            ObjectInputStream i = new ObjectInputStream(new BufferedInputStream(new FileInputStream(AUCTION_FILE)));
            auctionList = (ConcurrentHashMap<Integer, Auction>) i.readObject();
        }catch(FileNotFoundException e){
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();

        }catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void loadTimer() {
        try {
            ObjectInputStream i3 = new ObjectInputStream(new BufferedInputStream(new FileInputStream(TIMERS_FILE)));
            timerTasks = (HashMap<LifeCycleAuctionTask, Long>) i3.readObject();
        }catch(FileNotFoundException e){
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();

        }catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void loadUser() {
        try {
            ObjectInputStream i2 = new ObjectInputStream(new BufferedInputStream(new FileInputStream(USERS_FILE)));
            usersList = (ConcurrentHashMap<String, User>) i2.readObject();
        }catch(FileNotFoundException e){
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();

        }catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void loadClosedAuction() {
        try {
            ObjectInputStream i4 = new ObjectInputStream(new BufferedInputStream(new FileInputStream(CLOSED_AUCTION)));
            closedAuction = (HashMap<Integer, Auction>) i4.readObject();
        }catch(FileNotFoundException e){
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        }catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    /**
     * Reloads timer when loading auction from file
     */
    public void reloadTimer() {
        timer = new Timer();
        for (Map.Entry<LifeCycleAuctionTask, Long> t: timerTasks.entrySet()) {
            // Reschedule task to initial value subtracted how much has already elapsed
            t.getKey().passArgument(auctionList,closedAuction,timerTasks); // lo faccio per riallineare i riferimenti
            long timeLeft = t.getKey().getTimeLeft();
            if(timeLeft < 0) {
                t.getKey().run();
            }
            else {
                timer.schedule(t.getKey(), timeLeft);
            }
        }
    }

    public SystemAuctionHouse() throws RemoteException {
        usersList = new ConcurrentHashMap<>();
        usersList.put("alessio",new User("alessio","alessio"));
        auctionList = new ConcurrentHashMap<>();
        closedAuction = new HashMap<>();
        timer = new Timer();
        timerTasks = new HashMap<>();
    }

    public static void main(String[] args) throws RemoteException,ClassNotFoundException {
        SystemAuctionHouse sys = new SystemAuctionHouse();

            Registry reg = LocateRegistry.createRegistry(9999);
            reg.rebind("hii", sys);
            System.out.println("Server Ready");

            Scanner scn = new Scanner(System.in);
            while(true) {
                System.out.println("1)Carica da File   2)Salva su file");
                int decision = scn.nextInt();
                switch (decision) {
                    case 1:
                        System.out.println(sys.loadState());
                        break;
                    case 2:
                        System.out.println(sys.saveState());
                        break;
                    default:
                        break;

                }
            }

    }

}
