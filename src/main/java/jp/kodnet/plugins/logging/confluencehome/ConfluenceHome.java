package jp.kodnet.plugins.logging.confluencehome;

import com.atlassian.confluence.core.Beanable;
import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.security.GateKeeper;
import com.atlassian.confluence.setup.BootstrapManager;
import com.atlassian.confluence.util.ConfluenceHomeGlobalConstants;
import com.atlassian.spring.container.ContainerManager;
import com.opensymphony.webwork.ServletActionContext;
import com.opensymphony.xwork.ActionContext;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author trinhnk
 */
public class ConfluenceHome extends ConfluenceActionSupport implements Beanable {

    private BootstrapManager bootstrapManager = (BootstrapManager) ContainerManager.getComponent("bootstrapManager");
    private List<String> files;
    private GateKeeper gateKeeper = (GateKeeper) ContainerManager.getComponent("gateKeeper");
    private HashMap<String, byte[]> bytes;

    public GateKeeper getGateKeeper() {
        return gateKeeper;
    }

    @Override
    public String execute() throws Exception {
        if (bootstrapManager == null) {
            return ERROR;
        }
        File confluence = bootstrapManager.getLocalHome();
        String confluenceHome = confluence.getPath();
        File logsFolder = new File(confluenceHome + File.separator + ConfluenceHomeGlobalConstants.LOGS_DIR);
        if (!logsFolder.exists()) {
            return SUCCESS;
        }

        List<File> files = getFileOnFolder(logsFolder);
        this.files = new ArrayList<>();
        for (File file : files) {
            String path = file.getPath();
            this.files.add(URLEncoder.encode(path, "UTF-8"));
        }
        return SUCCESS;
    }

    public String doDownload() throws Exception {
        ActionContext context = ActionContext.getContext();
        Object object = context.getParameters().get("uri");
        if (object == null) {
            return null;
        }
        String uri = null;
        if (object instanceof String[] && ((String[]) object).length != 0) {
            uri = ((String[]) object)[0];
        } else if (object instanceof String) {
            uri = (String) object;
        }

        uri = URLDecoder.decode(uri, "UTF-8");
        uri = uri.replace("\\", "/");
        File file = new File(uri);
        byte[] bytes = Files.readAllBytes(file.toPath());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletResponse response = ServletActionContext.getResponse();
        response.setContentType("text/txt");
        String fileName = URLEncoder.encode(file.getName(), "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);

        ServletOutputStream outPut = response.getOutputStream();
        out.write(bytes);
        out.writeTo(outPut);
        outPut.flush();
        outPut.close();
        return null;
    }

    public List<File> getFileOnFolder(File root) {
        List<File> files = new ArrayList<File>();
        for (File file : root.listFiles()) {
            if (file.isDirectory()) {
                files.addAll(getFileOnFolder(file));
                continue;
            }
            files.add(file);
        }
        return files;
    }

    public List<String> getFiles() {
        return files;
    }

    @Override
    public Object getBean() {
        return this.bytes;
    }
}
