import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.Callable;

@Command (name="getDownloadCommand", mixinStandardHelpOptions = true, version = "Get Download Command 1.0",
        description = "Generate command lines for Youtube Chat Downloader")
public class GetDownloadCommand implements Callable<Integer> {

    @Option (names = {"-id", "--id"}, description = "Channel ID", defaultValue = "UC8rcEBzJSleTkf_-agPM20g")
    private String channelID;
    @Option (names = {"-b", "--before"}, description = "Published Before (yyyy-MM-dd)")
    private String endDate;
    @Option (names = {"-a", "--after"}, description = "Published After (yyyy-MM-dd)")
    private String beginningDate;
    @Option (names = {"-o", "--output"}, description = "Output File", defaultValue = "download-command.bat")
    private String outputFile;
    @Option (names = {"--od"}, description = "Output Directory", defaultValue = "")
    private String outputDirectory;
    @Option (names = {"-c", "--cn"}, description = "Channel Name", defaultValue = "IRyS")
    private String channelName;

    private static final String CLIENT_SECRETS= "client_secret.json";
    private static final Collection<String> SCOPES =
            Arrays.asList("https://www.googleapis.com/auth/youtube.force-ssl");

    private static final String APPLICATION_NAME = "Youtube Chat Downloader";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String IRYS_CHANNEL = "UC8rcEBzJSleTkf_-agPM20g";

    private List<String> PART_ITEMS = Arrays.asList("snippet", "id");
    private List<String> TYPE_ITEMS = Arrays.asList("video");
    private List<String> usedName = new ArrayList();

    // OAuth Credential
    public static Credential authorize(final NetHttpTransport httpTransport) throws IOException {
        InputStream in = GetDownloadCommand.class.getResourceAsStream(CLIENT_SECRETS);
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                        .build();

        Credential credential =
                new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        return credential;
    }

    public static YouTube getService() throws GeneralSecurityException, IOException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = authorize(httpTransport);
        return new YouTube.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    //  API Key
    public String getAPIKey() {
        String apiKey = "";

        try {
            Path apiKeyPath = Paths.get("C:/Users/Ngoc/workspace/GetDownloadCommand/src/main/resources/apikey.txt");
            apiKey = Files.readAllLines(apiKeyPath).get(0);
        } catch (Exception ex) {
            System.out.println("Error while retrieving API key: ");
            ex.printStackTrace();
        }
        return apiKey;
    }

    private String getCommandOutputFileName(String publishedDay) {
        String outputCommandName = "";
        int count = 1;

        LocalDate publishedDate = LocalDate.parse(publishedDay);
        String dayPublished = (new DecimalFormat("00")).format(publishedDate.getDayOfMonth());
        String monthPublished = publishedDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

        outputCommandName = channelName + "_" + dayPublished + monthPublished + ".json";
        while (usedName.contains(outputCommandName)) {
            count++;
            outputCommandName = channelName + "_" + dayPublished + monthPublished + "_" + count + ".json";
        }
        usedName.add(outputCommandName);

        System.out.println("Output File name: " + outputCommandName);

        return outputCommandName;
    }

    public void writeOutput(SearchListResponse response) {
        int count = 1;

        List<SearchResult> searchResultList = new ArrayList<>();
        if (response.getItems().size() > 0) {
            searchResultList.addAll(response.getItems());
            System.out.println("Channel: " + searchResultList.get(0).getSnippet().getChannelTitle());
            ArrayList<String> outputContent = new ArrayList<>();
            //  outputContent.add("Channel: " + searchResultList.get(0).getSnippet().getChannelTitle());
            try {
                Path path = Paths.get(outputFile);
                DateTime publishedDate;
                String publishedDay;
                String commandOutputFileName;

                for (SearchResult searchResult : searchResultList) {
                    publishedDate = searchResult.getSnippet().getPublishedAt();
                    publishedDay = convertDate(publishedDate.toString(), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    commandOutputFileName = getCommandOutputFileName(publishedDay);

                    System.out.println("ID: " + searchResult.getId().getVideoId());
                    //  outputContent.add("ID: " + searchResult.getId().getVideoId());
                    System.out.println("Title: " + searchResult.getSnippet().getTitle());
                    //  outputContent.add("Title: " + searchResult.getSnippet().getTitle());
                    System.out.println("Date: " + searchResult.getSnippet().getPublishedAt());
                    //  outputContent.add("Date: " + searchResult.getSnippet().getPublishedAt());
                    System.out.println("Command: chat_downloader https://www.youtube.com/watch?v=" + searchResult.getId().getVideoId()
                                                + " --cookies \"f:/chat-downloader/cookies.txt\" --message_groups \"messages superchat\" "
                                                + "--output \"" + outputDirectory
                                                + commandOutputFileName + "\" \n");
                    outputContent.add("chat_downloader https://www.youtube.com/watch?v=" + searchResult.getId().getVideoId()
                                    + " --cookies \"f:/chat-downloader/cookies.txt\" --message_groups \"messages superchat\" "
                                    + "--output \"" + outputDirectory
                                    + commandOutputFileName + "\""); // + searchResult.getSnippet().getPublishedAt()
                    System.out.println("==========");
                    Thread.sleep(2);
                }

                Files.write(path, outputContent);
            } catch (Exception ex) {
                System.out.println("Error while writing to file!");
                ex.printStackTrace();
            }
        }
    }

    private boolean validDateFormat(String dateString, String dateFormat) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateFormat);
        try {
            LocalDate localDate = LocalDate.parse(dateString, dtf);
        } catch (DateTimeParseException ex) {
            return false;
        }
        return true;
    }

    private String getDateFormat(String dateString) {
        if (dateString == null) {
            return "";
        }
        List<String> dateFormats = new ArrayList<String>(Arrays.asList( "yyyy-MM-dd",
                                                                        "yyyy/MM/dd",
                                                                        "dd-MM-yyyy",
                                                                        "dd/MM/yyyy"));
        for (String dateFormat : dateFormats) {
            if (validDateFormat(dateString, dateFormat)) {
                return dateFormat;
            }
        }
        return "";
    }

    private String convertDate(String inputDate, String inputDateFormat) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(inputDateFormat);
        LocalDate parsedDate = LocalDate.parse(inputDate, dtf);
        return parsedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    @Override
    public Integer call() throws GeneralSecurityException, IOException, GoogleJsonResponseException {

        if ((beginningDate == null) || (endDate == null) ||
                (getDateFormat(beginningDate).trim().isEmpty()) || (getDateFormat(endDate).trim().isEmpty())) {
            System.out.println("Error! Beginning / End Date missing or in incorrect format");
        } else {
            if (channelID == null) {
                System.out.println("Channel ID not specified.  Defaulting to IRyS' channel");
                channelID = IRYS_CHANNEL;
            }
            if (!(getDateFormat(beginningDate).equalsIgnoreCase("yyyy-MM-dd"))) {
                beginningDate = convertDate(beginningDate, getDateFormat(beginningDate));
                System.out.println("Beginning date: " + beginningDate);
            }
            if (!(getDateFormat(endDate).equalsIgnoreCase("yyyy-MM-dd"))) {
                endDate = convertDate(endDate, getDateFormat(endDate));
                System.out.println("End date: " + endDate);
            }
            if ((outputDirectory != null) && (!outputDirectory.isEmpty())) {
                Path outputPath = Paths.get(outputDirectory);
                if (!(Files.isDirectory(outputPath))) {
                    System.out.println("Output directory doesn't exist");
                    outputDirectory = "";
                } else {
                    if ((outputDirectory.charAt(outputDirectory.length() - 1) != '/') &&
                            (outputDirectory.charAt(outputDirectory.length() - 1) != '\\'))  {
                        outputDirectory = outputDirectory + "/";
                    }
                }
            }
            /*  OAuth
            YouTube youtubeService = getService();
            */
            //  API Key
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            YouTube youtubeAPIKeyService = new YouTube.Builder(httpTransport, JSON_FACTORY, new HttpRequestInitializer() {
                public void initialize(HttpRequest request) throws IOException {
                }
            }).setApplicationName(APPLICATION_NAME).build();
            //
            ArrayList<String>  partList = new ArrayList<>();
            partList.addAll(PART_ITEMS);
            ArrayList<String>  typeList = new ArrayList<>();
            typeList.addAll(TYPE_ITEMS);
            String bgDate = beginningDate + "T00:00:00.00Z";
            String eDate = endDate + "T00:00:00.00Z";
            /*
            IRyS' ID : UC8rcEBzJSleTkf_-agPM20g
            */
            YouTube.Search.List request = youtubeAPIKeyService.search()
                    .list(partList);
            SearchListResponse response = request.setKey(getAPIKey())
                    .setChannelId(channelID)
                    .setMaxResults(75L)
                    .setOrder("date")
                    .setPublishedAfter(bgDate)
                    .setPublishedBefore(eDate)
                    .setType(typeList)
                    .execute();

            writeOutput(response);
        }

        return 0;
    }

    public static void main(String[] args)
            throws GeneralSecurityException, IOException, GoogleJsonResponseException {
        GetDownloadCommand getDownloadCommand = new GetDownloadCommand();

        int exitCode = new CommandLine(getDownloadCommand).execute(args);
        System.exit(exitCode);
    }
}
