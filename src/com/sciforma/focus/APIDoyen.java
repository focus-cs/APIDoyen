/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sciforma.focus;

import com.sciforma.focus.beans.Connector;
import com.sciforma.focus.manager.ProjectManager;
import com.sciforma.focus.manager.ProjectManagerImpl;
import com.sciforma.psnext.api.InvalidDataException;
import com.sciforma.psnext.api.PSException;
import com.sciforma.psnext.api.Project;
import com.sciforma.psnext.api.ResAssignment;
import com.sciforma.psnext.api.Session;
import com.sciforma.psnext.api.Task;
import com.sciforma.psnext.api.TaskOutlineList;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.pmw.tinylog.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 *
 * @author lahou
 */
public class APIDoyen {

    private static final String VERSION = "1.00";

    public static ApplicationContext ctx;

    private static String IP;
    private static String PORT;
    private static String CONTEXTE;
    private static String USER;
    private static String PWD;

    public static Session mSession;
    private static ProjectManager projectManager;

    private static Date today;
    private static SimpleDateFormat sdf;

    public static void main(String[] args) {
        Logger.info("[main][V" + VERSION + "] Demarrage de l'API: " + new Date());
        try {
            initialisation();
            connection();
            chargementParametreSciforma();
            process();
            mSession.logout();
            Logger.info("[main][V" + VERSION + "] Fin de l'API: " + new Date());
        } catch (PSException ex) {
            Logger.error(ex);
        }
        System.exit(0);
    }

    private static void initialisation() {
        ctx = new FileSystemXmlApplicationContext(System.getProperty("user.dir") + System.getProperty("file.separator") + "config" + System.getProperty("file.separator") + "applicationContext.xml");
        sdf = new SimpleDateFormat("dd/MM/yyyy");
        today = Calendar.getInstance().getTime();
    }

    private static void connection() {
        Logger.info("[connexion] Chargement parametre connexion:" + new Date());
        Connector c = (Connector) ctx.getBean("sciforma");
        USER = c.getUSER();
        PWD = c.getPWD();
        IP = c.getIP();
        PORT = c.getPORT();
        CONTEXTE = c.getCONTEXTE();

        try {
            Logger.info("[connexion] Initialisation de la Session:" + new Date());
            String url = IP + "/" + CONTEXTE;
            Logger.info("[connexion] " + url + " :" + new Date());
            mSession = new Session(url);
            mSession.login(USER, PWD.toCharArray());
            Logger.info("[connexion] Connecté: " + new Date() + " Ă  l'instance " + CONTEXTE);
        } catch (PSException ex) {
            Logger.error("[connexion] Erreur dans la connection de ... " + CONTEXTE, ex);
            System.exit(-1);
        }
    }

    private static void chargementParametreSciforma() throws PSException {
        try {
            Logger.info("Demarrage du chargement des parametres de l'application:" + new Date());
            projectManager = new ProjectManagerImpl(mSession);
            //FILENAME_IMPORT_SAP = ((SciformaField) ctx.getBean("sap_to_sciforma")).getSciformaField();
            Logger.info("Fin du chargement des parametres de l'application:" + new Date());
        } catch (Exception ex) {
            Logger.error("Erreur dans la lecture l'intitialisation du parametrage " + new Date(), ex);
            mSession.logout();
            System.exit(1);
        }
    }

    private static void process() throws PSException {
        Project p = null;
        try {
            List<Project> lp = mSession.getProjectList(Project.VERSION_WORKING, Project.READWRITE_ACCESS);
            int nbProjet = lp.size();
            Iterator lpit = lp.iterator();
            while (lpit.hasNext()) {
                p = (Project) lpit.next();
                Logger.info("=======================================================================================");
                Logger.info("Traitement du projet [" + (lp.indexOf(p) + 1) + "/" + nbProjet + "] " + p.getStringField("Name"));
                Logger.info("=======================================================================================");
                try {
                    p.open(false);
                    if (p.getBooleanField("api_applytime")) {
                        if (today.compareTo(p.getDateField("Start")) > 0) {
                            p.applyTimesheets(p.getDateField("Start"), today, true, true, false);
                            Logger.info("Application des temps sur le projet " + p.getStringField("Name"));
                        } else {
                            Logger.warn("INVALID ! " + p.getStringField("Name") + " <" + sdf.format(p.getDateField("Start")) + "> et <" + sdf.format(today) + ">");
                        }
                    }
                    if (p.getBooleanField("api_clonage")) {
                        processArchivage(p);
                    } else {
                        p.save();
                    }
                    try {
                        p.publish();
                        Logger.info(p.getStringField("Name") + " a été publié");
                    } catch (PSException e) {
                        Logger.error("Le projet <" + p.getStringField("ID") + "> - <" + p.getStringField("Name") + " ne peut pas être publié car il ne respecte pas les critères du processus <" + p.getStringField("Workflow") + ">", e);
                    }
                    p.close();
                } catch (PSException ex) {
                    Logger.error(ex);
                }
            }
        } catch (PSException ex) {
            Logger.error(ex);
        } catch (Exception ex) {
            Logger.error(ex);
        } finally {
            p.close();
        }
    }

    private static void processArchivage(Project project) {
        try {
            Date beginYear = null;
            Calendar cObj;
            Date obj;
            Calendar d = Calendar.getInstance();
            Calendar d2 = Calendar.getInstance();
            if (d.get(Calendar.MONTH) > Calendar.JANUARY) {
                d.add(Calendar.YEAR, 1);
                d.set(Calendar.MONTH, Calendar.JANUARY);
                d2.add(Calendar.YEAR, 2);
                d2.set(Calendar.MONTH, Calendar.JANUARY);
            }
            d.set(Calendar.DATE, 1);
            d.set(Calendar.HOUR, 0);
            d.set(Calendar.MINUTE, 0);
            d.set(Calendar.SECOND, 0);
            d.set(Calendar.MILLISECOND, 0);
            d2.set(Calendar.DATE, 1);
            d2.set(Calendar.HOUR, 0);
            d2.set(Calendar.MINUTE, 0);
            d2.set(Calendar.SECOND, 0);
            d2.set(Calendar.MILLISECOND, 0);
            beginYear = d.getTime();

            Double dou = Math.random();

            project.setBooleanField("api_clonage", false);
            project.save();
            try {
                project.publish();
                Logger.info(project.getStringField("Name") + " a été publié");
            } catch (PSException e) {
                Logger.error("Le projet <" + project.getStringField("ID") + "> - <" + project.getStringField("Name") + " ne peut pas être publié car il ne respecte pas les critères du processus <" + project.getStringField("Workflow") + ">", e);
            }
            project.close();
            project.open(true);

            project.setStringField("ID", "API" + dou);
            project.saveAs(Project.VERSION_TEMPLATE);
            Logger.info(project.getStringField("Name") + " a été enregistré comme template.");

            String template = project.getStringField("ID");
            String projName = "_" + dou.toString() + "_" + project.getStringField("Name");
            String projID = dou.toString();

            Project newProject = new Project(projName, projID, Project.VERSION_WORKING);
            Logger.info("Création d'un nouveau projet: " + projName + " avec comme ID: " + projID);
            newProject.setStringField("Type", project.getStringField("Type"));
            newProject.setDateField("Start Constraint", beginYear);
            newProject.setBooleanField("Actuals from Tracking Source Only", false);
            newProject.setStringField("Manager 1", project.getStringField("Manager 1"));
            newProject.setStringField("Manager 2", project.getStringField("Manager 2"));
            newProject.setStringField("Manager 3", project.getStringField("Manager 3"));
            if (project.getListField("Performing Organizations").size() > 0) {
                newProject.setListField("Performing Organizations", project.getListField("Performing Organizations"));
            }
            newProject.setStringField("Portfolio Folder", project.getStringField("Portfolio Folder"));
            newProject.setStringField("Owning Organization", project.getStringField("Owning Organization"));
            project.close();
            Logger.info("Fermeture du template.");
            newProject.saveAs(Project.VERSION_WORKING);
            Logger.info(newProject.getStringField("Name") + " a été crée.");
            newProject.insertTemplate(template, null);
            Logger.info("Insertion du template");
            newProject.save();
            Logger.info(newProject.getStringField("Name") + " a été sauvegardé.");

            TaskOutlineList newtasks = newProject.getTaskOutlineList();
            Iterator taskIt = newtasks.iterator();
            while (taskIt.hasNext()) {
                Task t = (Task) taskIt.next();
                if (t.getDateField("Required Date").getTime() != -1) {
                    obj = t.getDateField("Required Date");
                    cObj = Calendar.getInstance();
                    cObj.setTime(obj);
                    cObj.add(Calendar.YEAR, 1);
                    t.setDateField("Required Date", cObj.getTime());
                }
                if (!t.getBooleanField("Is Parent")) {
                    List<ResAssignment> resAssignList = t.getResAssignmentList();
                    Iterator resIt = resAssignList.iterator();
                    while (resIt.hasNext()) {
                        ResAssignment res = (ResAssignment) resIt.next();
                        Double raf = res.getDoubleField("Remaining Effort");
                        res.setDoubleField("Actual Effort", 0.0);
                        res.setDoubleField("Total Effort", raf);
                    }
                    t.setDoubleField("% Completed", 0.0);
                    if (t.getBooleanField("Closed")) {
                        t.setDoubleField("Duration", 0.0);
                        t.setDateField("Finish", beginYear);
                    }
                    if (t.getDoubleField("Start Delay") > 0.0) {
                        t.setDoubleField("Start Delay", 0.0);
                    }
                }
            }
            newProject.updateProgress(beginYear, false, true);
            Logger.info("updateProgress du projet");
            newProject.setBooleanField("Actuals from Tracking Source Only", false);
            newProject.setBooleanField("api_clonage", false);
            newProject.save();
            //SUPRESSION ACTIVITE CLOTUREE INUTILE
            Boolean supressionFinie = false;
            while (!supressionFinie) {
                supressionFinie = true;
//                newtasks = newProject.getTaskOutlineList();
                List<Task> lta = new ArrayList<Task>();
                taskIt = newtasks.iterator();
                while (taskIt.hasNext()) {
                    Task t = (Task) taskIt.next();
                    if (!t.getBooleanField("Is Parent")) {
                        if (t.getBooleanField("Closed")) {
                            if (t.getBooleanField("Parent Clôturé")) {
                                lta.add(t);
                                supressionFinie = false;
                            } else {
                                if (t.getSuccessorLinksList().isEmpty() && t.getPredecessorLinksList().isEmpty()) {
                                    lta.add(t);
                                    supressionFinie = false;
                                }
                            }
                        }
                    }
                }

                for (Task task : lta) {
                    newtasks.remove(task);
                }
            }
            newProject.save();
            newProject.close();
            Logger.info("Fermeture du nouveau projet");
            project.deactivate();
            Logger.info("Archivage du template");

        } catch (InvalidDataException ex) {
            Logger.error(null, ex);
        } catch (PSException ex) {
            Logger.error(null, ex);
        }
    }
}
